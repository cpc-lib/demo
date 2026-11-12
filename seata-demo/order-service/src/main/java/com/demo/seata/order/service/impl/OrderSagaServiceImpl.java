package com.demo.seata.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.api.saga.AccountSagaDubboService;
import com.demo.seata.api.saga.StorageSagaDubboService;
import com.demo.seata.common.annotation.Idempotent;
import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.mapper.OrderInfoMapper;
import com.demo.seata.order.service.OrderSagaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * SAGA 模式 - 订单创建实现 (长事务正向调用 + 逆向幂等补偿编排)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderSagaServiceImpl implements OrderSagaService {

    private final OrderInfoMapper orderInfoMapper;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private StorageSagaDubboService storageSagaDubboService;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private AccountSagaDubboService accountSagaDubboService;

    @Override
    @Idempotent(key = "#request.orderNo", bizType = "ORDER_CREATE_SAGA")
    public OrderInfo createOrder(OrderCreateRequest request) {
        String businessKey = request.getOrderNo();
        log.info("[SAGA模式 - 开启SAGA编排] businessKey: {}, 订单号: {}, 用户: {}, 商品: {}",
                businessKey, request.getOrderNo(), request.getUserId(), request.getCommodityCode());

        // 1. 业务防重判断: 检查该 orderNo 是否已入库
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();
        query.eq(OrderInfo::getOrderNo, request.getOrderNo());
        OrderInfo existingOrder = orderInfoMapper.selectOne(query);
        if (existingOrder != null) {
            log.info("[SAGA模式 - 订单创建] 订单已存在，幂等返回历史订单: {}", existingOrder.getOrderNo());
            return existingOrder;
        }

        boolean storageDeducted = false;
        boolean accountDebited = false;
        OrderInfo orderInfo = null;

        try {
            // 2. 执行正向动作 1: 扣减库存
            log.info("[SAGA模式 - 正向步骤1] 调用库存服务扣减库存: 商品 {}, 数量 {}", request.getCommodityCode(), request.getCount());
            storageDeducted = storageSagaDubboService.reduceStock(businessKey, request.getCommodityCode(), request.getCount());
            if (!storageDeducted) {
                throw new BusinessException("SAGA正向扣减库存失败");
            }

            // 3. 执行正向动作 2: 扣减余额
            log.info("[SAGA模式 - 正向步骤2] 调用账户服务扣减余额: 用户 {}, 金额 {}", request.getUserId(), request.getMoney());
            accountDebited = accountSagaDubboService.reduceBalance(businessKey, request.getUserId(), request.getMoney());
            if (!accountDebited) {
                throw new BusinessException("SAGA正向扣减余额失败");
            }

            // 4. 执行正向动作 3: 创建本地订单
            orderInfo = OrderInfo.builder()
                    .orderNo(request.getOrderNo())
                    .userId(request.getUserId())
                    .commodityCode(request.getCommodityCode())
                    .count(request.getCount())
                    .money(request.getMoney())
                    .status("SUCCESS")
                    .mode("SAGA")
                    .createTime(LocalDateTime.now())
                    .updateTime(LocalDateTime.now())
                    .build();
            orderInfoMapper.insert(orderInfo);
            log.info("[SAGA模式 - 正向步骤3] 本地订单记录已保存: {}", orderInfo.getOrderNo());

            // 5. 模拟异常触发补偿测试
            if (Boolean.TRUE.equals(request.getMockException())) {
                log.error("[SAGA模式 - 模拟测试异常] 主动触发逆向补偿流程! businessKey: {}", businessKey);
                throw new BusinessException("SAGA模式模拟测试异常，进入全链路逆向补偿流程！");
            }

            log.info("[SAGA模式 - 事务完成] 正向链路全部成功完成, businessKey: {}", businessKey);
            return orderInfo;

        } catch (Exception ex) {
            log.warn("[SAGA模式 - 逆向补偿] 检测到异常: {}, 启动逆向补偿链路! businessKey: {}", ex.getMessage(), businessKey);

            // 逆向补偿 1: 补偿账户余额 (账户服务内部具备幂等保证)
            if (accountDebited) {
                try {
                    log.info("[SAGA模式 - 补偿步骤1] 补偿账户余额: 用户 {}, 金额 {}", request.getUserId(), request.getMoney());
                    accountSagaDubboService.compensateBalance(businessKey, request.getUserId(), request.getMoney());
                } catch (Exception e) {
                    log.error("[SAGA模式 - 补偿步骤1] 账户补偿失败，需重试或人工介入! businessKey: {}", businessKey, e);
                }
            }

            // 逆向补偿 2: 补偿库存 (库存服务内部具备幂等保证)
            if (storageDeducted) {
                try {
                    log.info("[SAGA模式 - 补偿步骤2] 补偿库存: 商品 {}, 数量 {}", request.getCommodityCode(), request.getCount());
                    storageSagaDubboService.compensateStock(businessKey, request.getCommodityCode(), request.getCount());
                } catch (Exception e) {
                    log.error("[SAGA模式 - 补偿步骤2] 库存补偿失败，需重试或人工介入! businessKey: {}", businessKey, e);
                }
            }

            // 逆向补偿 3: 更新订单状态为 CANCELLED
            if (orderInfo != null && orderInfo.getId() != null) {
                orderInfo.setStatus("CANCELLED");
                orderInfo.setUpdateTime(LocalDateTime.now());
                orderInfoMapper.updateById(orderInfo);
                log.info("[SAGA模式 - 补偿步骤3] 订单状态已更新为 CANCELLED: {}", orderInfo.getOrderNo());
            }

            throw new BusinessException("SAGA执行失败并已完成全链路逆向幂等补偿: " + ex.getMessage(), ex);
        }
    }
}
