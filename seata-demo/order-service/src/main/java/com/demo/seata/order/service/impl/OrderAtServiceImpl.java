package com.demo.seata.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.api.at.AccountDubboService;
import com.demo.seata.api.at.StorageDubboService;
import com.demo.seata.common.annotation.Idempotent;
import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.mapper.OrderInfoMapper;
import com.demo.seata.order.service.OrderAtService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * AT 模式 - 订单创建实现 (TM 发起端)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderAtServiceImpl implements OrderAtService {

    private final OrderInfoMapper orderInfoMapper;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private StorageDubboService storageDubboService;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private AccountDubboService accountDubboService;

    @Override
    @Idempotent(key = "#request.orderNo", bizType = "ORDER_CREATE_AT")
    @GlobalTransactional(name = "create-order-at", rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderCreateRequest request) {
        String xid = RootContext.getXID();
        log.info("[AT模式 - 开启全局事务] XID: {}, 订单号: {}, 用户: {}, 商品: {}",
                xid, request.getOrderNo(), request.getUserId(), request.getCommodityCode());

        // 1. 业务防重判断: 检查该 orderNo 是否已入库
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();
        query.eq(OrderInfo::getOrderNo, request.getOrderNo());
        OrderInfo existingOrder = orderInfoMapper.selectOne(query);
        if (existingOrder != null) {
            log.info("[AT模式 - 订单创建] 订单已存在，幂等返回历史订单: {}", existingOrder.getOrderNo());
            return existingOrder;
        }

        // 2. 调用库存服务扣减库存 (AT 分支事务 1)
        log.info("[AT模式 - 步骤1] 调用库存服务扣减库存: 商品 {}, 数量 {}", request.getCommodityCode(), request.getCount());
        storageDubboService.deduct(request.getCommodityCode(), request.getCount());

        // 3. 调用账户服务扣减余额 (AT 分支事务 2)
        log.info("[AT模式 - 步骤2] 调用账户服务扣款: 用户 {}, 金额 {}", request.getUserId(), request.getMoney());
        accountDubboService.debit(request.getUserId(), request.getMoney());

        // 4. 保存本地订单 (AT 分支事务 3)
        OrderInfo orderInfo = OrderInfo.builder()
                .orderNo(request.getOrderNo())
                .userId(request.getUserId())
                .commodityCode(request.getCommodityCode())
                .count(request.getCount())
                .money(request.getMoney())
                .status("SUCCESS")
                .mode("AT")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        orderInfoMapper.insert(orderInfo);
        log.info("[AT模式 - 步骤3] 保存本地订单成功: {}", orderInfo.getOrderNo());

        // 5. 模拟异常测试: 若请求指定 mockException=true，主动抛出异常触发全局回滚
        if (Boolean.TRUE.equals(request.getMockException())) {
            log.error("[AT模式 - 模拟测试异常] 触发全局事务回滚! XID: {}", xid);
            throw new BusinessException("AT模式模拟测试异常，触发全部微服务回滚！");
        }

        log.info("[AT模式 - 全局事务完成] 提交成功, XID: {}", xid);
        return orderInfo;
    }
}
