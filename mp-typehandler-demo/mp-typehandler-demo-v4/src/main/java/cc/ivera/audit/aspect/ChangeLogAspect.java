package cc.ivera.audit.aspect;

import cc.ivera.audit.annotation.ChangeLog;
import cc.ivera.audit.annotation.LogField;
import cc.ivera.audit.enums.ChangeLogMode;
import cc.ivera.audit.model.TrackedFieldDefinition;
import cc.ivera.audit.provider.ChangeLogSnapshotProvider;
import cc.ivera.service.audit.ChangeLogService;
import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class ChangeLogAspect {

    private final List<ChangeLogSnapshotProvider> snapshotProviders;
    private final ChangeLogService changeLogService;

    private final DefaultParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    private final ExpressionParser expressionParser = new SpelExpressionParser();

    @Around("@annotation(changeLog)")
    public Object around(ProceedingJoinPoint joinPoint, ChangeLog changeLog) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        StandardEvaluationContext context = buildContext(method, joinPoint.getArgs());

        Object bizId = evaluateExpression(changeLog.bizId(), context);
        ChangeLogSnapshotProvider provider = findProvider(changeLog.entityClass());
        Object before = provider.loadById(bizId);
        Object beforeCopy = cloneObject(before, changeLog.entityClass());

        Object result = joinPoint.proceed();
        if (!isSuccess(result)) {
            return result;
        }

        Object after = provider.loadById(bizId);
        if (after == null) {
            log.warn("变更日志记录跳过，更新后对象不存在, bizType={}, bizId={}", changeLog.bizType(), bizId);
            return result;
        }

        List<TrackedFieldDefinition> trackedFields = resolveTrackedFields(changeLog);
        if (CollectionUtils.isEmpty(trackedFields)) {
            log.warn("变更日志记录跳过，未配置任何跟踪字段, method={}", method.getName());
            return result;
        }

        String operator = stringifyExpression(changeLog.operator(), context, "system");
        String requestUri = stringifyExpression(changeLog.requestUri(), context, "");

        changeLogService.recordUpdate(
                changeLog.bizType(),
                String.valueOf(bizId),
                operator,
                method.getName(),
                requestUri,
                changeLog.mode().name(),
                beforeCopy,
                after,
                trackedFields
        );
        return result;
    }

    private StandardEvaluationContext buildContext(Method method, Object[] args) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setVariable("args", args);
        context.setVariable("now", LocalDateTime.now());
        return context;
    }

    private Object evaluateExpression(String expressionText, StandardEvaluationContext context) {
        Expression expression = expressionParser.parseExpression(expressionText);
        return expression.getValue(context);
    }

    private String stringifyExpression(String expressionText, StandardEvaluationContext context, String defaultValue) {
        Object value = evaluateExpression(expressionText, context);
        return value == null ? defaultValue : String.valueOf(value);
    }

    private ChangeLogSnapshotProvider findProvider(Class<?> entityClass) {
        return snapshotProviders.stream()
                .filter(item -> item.supports(entityClass))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到变更日志快照提供者: " + entityClass.getName()));
    }

    private boolean isSuccess(Object result) {
        if (result == null) {
            return true;
        }
        if (result instanceof Boolean bool) {
            return bool;
        }
        return true;
    }

    private Object cloneObject(Object source, Class<?> entityClass) {
        if (source == null) {
            return null;
        }
        return JSON.parseObject(JSON.toJSONString(source), entityClass);
    }

    private List<TrackedFieldDefinition> resolveTrackedFields(ChangeLog changeLog) {
        if (changeLog.mode() == ChangeLogMode.HARDCODED) {
            return Arrays.stream(changeLog.hardcodedFields())
                    .filter(item -> item != null && !item.isBlank())
                    .map(this::parseTrackedField)
                    .toList();
        }
        return resolveFromAnnotation(changeLog.entityClass());
    }

    private TrackedFieldDefinition parseTrackedField(String config) {
        String[] parts = config.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException("hardcodedFields 配置格式错误，应为 fieldName:字段名，实际值=" + config);
        }
        return new TrackedFieldDefinition(parts[0].trim(), parts[1].trim());
    }

    private List<TrackedFieldDefinition> resolveFromAnnotation(Class<?> entityClass) {
        List<TrackedFieldDefinition> fields = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            LogField logField = field.getAnnotation(LogField.class);
            if (logField != null && logField.enabled()) {
                fields.add(new TrackedFieldDefinition(field.getName(), logField.label()));
            }
        }
        return fields;
    }
}
