package com.demo.seata.common.aop;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.demo.seata.common.annotation.Idempotent;
import com.demo.seata.common.domain.entity.IdempotentRecord;
import com.demo.seata.common.enums.IdempotentStatusEnum;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.common.mapper.IdempotentRecordMapper;
import com.demo.seata.common.vo.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 接口与业务幂等切面
 * 通过数据库唯一约束 + 状态机 (PROCESSING -> SUCCESS / FAILED) 保证幂等执行
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.LOWEST_PRECEDENCE - 1)
public class IdempotentAspect {

    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final DefaultParameterNameDiscoverer discoverer = new DefaultParameterNameDiscoverer();

    @Autowired(required = false)
    private IdempotentRecordMapper idempotentRecordMapper;

    @Around("@annotation(idempotent)")
    public Object handleIdempotent(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        if (idempotentRecordMapper == null) {
            log.warn("IdempotentRecordMapper not found, skip database idempotency check");
            return joinPoint.proceed();
        }

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();

        // 1. 解析 SpEL 获取幂等 Key
        String idempotentKey = parseSpelKey(idempotent.key(), method, args);
        if (!StringUtils.hasText(idempotentKey)) {
            log.warn("Idempotent key resolved to empty, fallback to normal proceed");
            return joinPoint.proceed();
        }

        String bizType = idempotent.bizType();
        log.info("[幂等校验] 检查幂等Key: {}, 业务类型: {}", idempotentKey, bizType);

        // 2. 查询幂等记录
        LambdaQueryWrapper<IdempotentRecord> query = new LambdaQueryWrapper<>();
        query.eq(IdempotentRecord::getIdempotentKey, idempotentKey);
        IdempotentRecord record = idempotentRecordMapper.selectOne(query);

        if (record != null) {
            if (IdempotentStatusEnum.PROCESSING.getCode().equals(record.getStatus())) {
                log.warn("[幂等拦截] 任务正在处理中, Key: {}", idempotentKey);
                throw new BusinessException(idempotent.message());
            } else if (IdempotentStatusEnum.SUCCESS.getCode().equals(record.getStatus())) {
                log.info("[幂等命中] 任务已成功处理过, 直接返回历史结果, Key: {}", idempotentKey);
                Class<?> returnType = method.getReturnType();
                if (StringUtils.hasText(record.getResponseData())) {
                    return JSON.parseObject(record.getResponseData(), returnType);
                }
                return Result.success();
            } else {
                log.info("[幂等重试] 历史处理失败, 允许重新执行, Key: {}", idempotentKey);
                // 更新状态为处理中
                LambdaUpdateWrapper<IdempotentRecord> updateWrapper = new LambdaUpdateWrapper<>();
                updateWrapper.eq(IdempotentRecord::getIdempotentKey, idempotentKey)
                        .set(IdempotentRecord::getStatus, IdempotentStatusEnum.PROCESSING.getCode())
                        .set(IdempotentRecord::getUpdateTime, LocalDateTime.now());
                idempotentRecordMapper.update(null, updateWrapper);
            }
        } else {
            // 首次插入处理中状态
            IdempotentRecord newRecord = IdempotentRecord.builder()
                    .idempotentKey(idempotentKey)
                    .bizType(bizType)
                    .status(IdempotentStatusEnum.PROCESSING.getCode())
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            try {
                idempotentRecordMapper.insert(newRecord);
            } catch (DuplicateKeyException e) {
                log.warn("[并发拦截] 并发重复提交, Key: {}", idempotentKey);
                throw new BusinessException(idempotent.message());
            }
        }

        // 3. 执行目标方法
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            // 标记为失败，允许补偿或重试
            LambdaUpdateWrapper<IdempotentRecord> failWrapper = new LambdaUpdateWrapper<>();
            failWrapper.eq(IdempotentRecord::getIdempotentKey, idempotentKey)
                    .set(IdempotentRecord::getStatus, IdempotentStatusEnum.FAILED.getCode())
                    .set(IdempotentRecord::getUpdateTime, LocalDateTime.now());
            idempotentRecordMapper.update(null, failWrapper);
            throw ex;
        }

        // 4. 执行成功，更新状态为 SUCCESS 并缓存响应数据
        LambdaUpdateWrapper<IdempotentRecord> successWrapper = new LambdaUpdateWrapper<>();
        successWrapper.eq(IdempotentRecord::getIdempotentKey, idempotentKey)
                .set(IdempotentRecord::getStatus, IdempotentStatusEnum.SUCCESS.getCode())
                .set(IdempotentRecord::getResponseData, result != null ? JSON.toJSONString(result) : null)
                .set(IdempotentRecord::getUpdateTime, LocalDateTime.now());
        idempotentRecordMapper.update(null, successWrapper);

        return result;
    }

    private String parseSpelKey(String spelExpression, Method method, Object[] args) {
        String[] params = discoverer.getParameterNames(method);
        if (params == null || params.length == 0) {
            return null;
        }
        EvaluationContext context = new StandardEvaluationContext();
        for (int i = 0; i < params.length; i++) {
            context.setVariable(params[i], args[i]);
        }
        try {
            Expression expression = parser.parseExpression(spelExpression);
            Object value = expression.getValue(context);
            return value != null ? value.toString() : null;
        } catch (Exception e) {
            log.error("Failed to parse SpEL expression: {}", spelExpression, e);
            return null;
        }
    }
}
