package com.demo.seata.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.demo.seata.api.tcc.AccountTccDubboService;
import com.demo.seata.api.tcc.StorageTccDubboService;
import com.demo.seata.common.annotation.Idempotent;
import com.demo.seata.common.domain.dto.OrderCreateRequest;
import com.demo.seata.common.exception.BusinessException;
import com.demo.seata.order.entity.OrderInfo;
import com.demo.seata.order.mapper.OrderInfoMapper;
import com.demo.seata.order.service.OrderTccService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * TCC 模式 - 订单创建实现 (TM 发起端)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderTccServiceImpl implements OrderTccService {

    private final OrderInfoMapper orderInfoMapper;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private StorageTccDubboService storageTccDubboService;

    @DubboReference(check = false, timeout = 10000, retries = 0)
    private AccountTccDubboService accountTccDubboService;

    @Override
    @Idempotent(key = "#request.orderNo", bizType = "ORDER_CREATE_TCC")
    @GlobalTransactional(name = "create-order-tcc", rollbackFor = Exception.class)
    public OrderInfo createOrder(OrderCreateRequest request) {
        String xid = RootContext.getXID();
        log.info("[TCC模式 - 开启全局事务] XID: {}, 订单号: {}, 用户: {}, 商品: {}",
                xid, request.getOrderNo(), request.getUserId(), request.getCommodityCode());

        // 1. 业务防重判断: 检查该 orderNo 是否已入库
        LambdaQueryWrapper<OrderInfo> query = new LambdaQueryWrapper<>();
        query.eq(OrderInfo::getOrderNo, request.getOrderNo());
        OrderInfo existingOrder = orderInfoMapper.selectOne(query);
        if (existingOrder != null) {
            log.info("[TCC模式 - 订单创建] 订单已存在，幂等返回历史订单: {}", existingOrder.getOrderNo());
            return existingOrder;
        }

        // 2. 调用库存服务 TCC Try: 预留冻结库存
        log.info("[TCC模式 - 步骤1] 调用库存服务 Try: 商品 {}, 冻结数量 {}", request.getCommodityCode(), request.getCount());
        boolean storagePrepared = storageTccDubboService.prepare(null, request.getCommodityCode(), request.getCount());
        if (!storagePrepared) {
            throw new BusinessException("库存预留失败，触发回滚");
        }

        // 3. 调用账户服务 TCC Try: 预留冻结金额
        log.info("[TCC模式 - 步骤2] 调用账户服务 Try: 用户 {}, 冻结金额 {}", request.getUserId(), request.getMoney());
        boolean accountPrepared = accountTccDubboService.prepare(null, request.getUserId(), request.getMoney());
        if (!accountPrepared) {
            throw new BusinessException("账户余额预留失败，触发回滚");
        }

        // 4. 保存本地订单
        OrderInfo orderInfo = OrderInfo.builder()
                .orderNo(request.getOrderNo())
                .userId(request.getUserId())
                .commodityCode(request.getCommodityCode())
                .count(request.getCount())
                .money(request.getMoney())
                .status("SUCCESS")
                .mode("TCC")
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();
        orderInfoMapper.insert(orderInfo);
        log.info("[TCC模式 - 步骤3] 本地订单记录已保存: {}", orderInfo.getOrderNo());

        // 5. 模拟异常测试: 触发全局回滚 (TC 自动调用库存与账户的 Cancel 释放预留资源)
        if (Boolean.TRUE.equals(request.getMockException())) {
            log.error("[TCC模式 - 模拟测试异常] 触发全局 Cancel 回滚! XID: {}", xid);
            throw new BusinessException("TCC模式模拟测试异常，触发全部微服务 Cancel 回滚！");
        }

        log.info("[TCC模式 - 全局事务完成] 一阶段 Try 全部成功，TC 即将异步调用 Confirm, XID: {}", xid);
        return orderInfo;
    }
}
