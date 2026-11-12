package com.demo.seata.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.api.xa.AccountXaDubboService;
import com.demo.seata.api.xa.StorageXaDubboService;
import com.demo.seata.common.annotation.Idempotent;
import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.mapper.OrderInfoMapper;
import com.demo.seata.order.service.OrderXaService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * XA 模式 - 订单创建实现 (TM 发起端，强一致性 2PC)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderXaServiceImpl implements OrderXaService {

    private final OrderInfoMapper orderInfoMapper;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private StorageXaDubboService storageXaDubboService;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private AccountXaDubboService accountXaDubboService;

    @Override
    @Idempotent(key = "#request.orderNo", bizType = "ORDER_CREATE_XA")
    @GlobalTransactional(name = "create-order-xa", rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderCreateRequest request) {
        String xid = RootContext.getXID();
        log.info("[XA模式 - 开启全局强一致性事务] XID: {}, 订单号: {}, 用户: {}, 商品: {}",
                xid, request.getOrderNo(), request.getUserId(), request.getCommodityCode());

        // 1. 业务防重判断: 检查该 orderNo 是否已入库
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();
        query.eq(OrderInfo::getOrderNo, request.getOrderNo());
        OrderInfo existingOrder = orderInfoMapper.selectOne(query);
        if (existingOrder != null) {
            log.info("[XA模式 - 订单创建] 订单已存在，幂等返回历史订单: {}", existingOrder.getOrderNo());
            return existingOrder;
        }

        // 2. 调用库存服务 XA 分支扣减
        log.info("[XA模式 - 步骤1] 调用库存服务扣减库存: 商品 {}, 数量 {}", request.getCommodityCode(), request.getCount());
        storageXaDubboService.deduct(request.getCommodityCode(), request.getCount());

        // 3. 调用账户服务 XA 分支扣款
        log.info("[XA模式 - 步骤2] 调用账户服务扣款: 用户 {}, 金额 {}", request.getUserId(), request.getMoney());
        accountXaDubboService.debit(request.getUserId(), request.getMoney());

        // 4. 保存本地订单
        OrderInfo orderInfo = OrderInfo.builder()
                .orderNo(request.getOrderNo())
                .userId(request.getUserId())
                .commodityCode(request.getCommodityCode())
                .count(request.getCount())
                .money(request.getMoney())
                .status("SUCCESS")
                .mode("XA")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        orderInfoMapper.insert(orderInfo);
        log.info("[XA模式 - 步骤3] 本地订单记录已保存: {}", orderInfo.getOrderNo());

        // 5. 模拟异常触发 XA 2PC 全局回滚
        if (Boolean.TRUE.equals(request.getMockException())) {
            log.error("[XA模式 - 模拟测试异常] 触发 XA 2PC 全局回滚! XID: {}", xid);
            throw new BusinessException("XA模式模拟测试异常，触发全部微服务 2PC 回滚！");
        }

        log.info("[XA模式 - 全局事务完成] 提交成功, XID: {}", xid);
        return orderInfo;
    }
}
