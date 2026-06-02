package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private static final long ORDER_CREATE_LOCK_WAIT_MS = 3000L;
    private static final long ORDER_CREATE_LOCK_LEASE_MS = 10000L;

    private final ProductMapper productMapper;

    private final OrderCloseMessageService orderCloseMessageService;

    private final DistributedLockTemplate distributedLockTemplate;

    private final TransactionTemplate transactionTemplate;

    public OrderInfoServiceImpl(
        ProductMapper productMapper,
        OrderCloseMessageService orderCloseMessageService,
        DistributedLockTemplate distributedLockTemplate,
        TransactionTemplate transactionTemplate
    ) {
        this.productMapper = productMapper;
        this.orderCloseMessageService = orderCloseMessageService;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public OrderInfo createOrReuseOrder(Long productId, String paymentType) {
        validateCreateOrderParams(productId, paymentType);

        String lockKey = buildCreateOrderLockKey(productId, paymentType);
        return distributedLockTemplate.execute(
                lockKey,
                ORDER_CREATE_LOCK_WAIT_MS,
                ORDER_CREATE_LOCK_LEASE_MS,
                () -> transactionTemplate.execute(status -> doCreateOrReuseOrder(productId, paymentType))
        );
    }

    private OrderInfo doCreateOrReuseOrder(Long productId, String paymentType) {
        // 第一层防护：Redis 分布式锁，挡住多实例并发。
        // 第二层防护：select ... for update，挡住同库事务并发。
        OrderInfo noPayOrder = baseMapper.selectNoPayOrderForUpdate(
                productId,
                paymentType,
                OrderStatus.NOTPAY.getType()
        );
        if (noPayOrder != null) {
            log.info("复用未支付订单，productId={}, paymentType={}, orderNo={}",
                    productId, paymentType, noPayOrder.getOrderNo());
            return noPayOrder;
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("商品不存在");
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);
        orderInfo.setVersion(0);

        try {
            baseMapper.insert(orderInfo);
        } catch (DuplicateKeyException e) {
            // 极小概率订单号碰撞，或者历史数据存在并发写入时，兜底重新查询未支付订单。
            log.warn("订单插入触发唯一约束，尝试复用已有订单，productId={}, paymentType={}", productId, paymentType, e);
            OrderInfo existOrder = baseMapper.selectNoPayOrderForUpdate(
                    productId,
                    paymentType,
                    OrderStatus.NOTPAY.getType()
            );
            if (existOrder != null) {
                return existOrder;
            }
            throw e;
        }

        // 订单成功落库后再发送延迟关单消息，避免消息先于事务提交。
        sendCloseOrderMessageAfterCommit(orderInfo);
        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(codeUrl)) {
            return;
        }

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .and(wrapper -> wrapper.isNull("code_url").or().eq("code_url", ""));

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        int updated = baseMapper.update(orderInfo, updateWrapper);
        if (updated == 0) {
            log.info("订单二维码已存在，本次不覆盖，orderNo={}", orderNo);
        }
    }

    @Override
    public List<OrderInfo> listOrderByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>().orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        if (!StringUtils.hasText(orderNo) || orderStatus == null) {
            return;
        }
        log.info("更新订单状态 ===> orderNo={}, status={}", orderNo, orderStatus.getType());

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());

        baseMapper.update(orderInfo, updateWrapper);
    }

    @Override
    public boolean updateStatusByOrderNoIfStatus(String orderNo, OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!StringUtils.hasText(orderNo) || currentStatus == null || targetStatus == null) {
            return false;
        }

        log.info("条件更新订单状态 ===> orderNo={}, {} -> {}",
                orderNo,
                currentStatus.getType(),
                targetStatus.getType());

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);
        updateWrapper.eq("order_status", currentStatus.getType());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(targetStatus.getType());

        return baseMapper.update(orderInfo, updateWrapper) > 0;
    }

    @Override
    public String getOrderStatus(String orderNo) {
        OrderInfo orderInfo = getOrderByOrderNo(orderNo);
        return orderInfo == null ? null : orderInfo.getOrderStatus();
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo).last("limit 1");
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNoForUpdate(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return baseMapper.selectByOrderNoForUpdate(orderNo);
    }

    private void sendCloseOrderMessageAfterCommit(OrderInfo orderInfo) {
        if (orderInfo == null || !StringUtils.hasText(orderInfo.getOrderNo())) {
            return;
        }

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            orderCloseMessageService.sendCloseOrderMessage(orderInfo.getOrderNo(), orderInfo.getPaymentType());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                orderCloseMessageService.sendCloseOrderMessage(orderInfo.getOrderNo(), orderInfo.getPaymentType());
            }
        });
    }

    private void validateCreateOrderParams(Long productId, String paymentType) {
        if (productId == null) {
            throw new BizException("商品ID不能为空");
        }
        if (!StringUtils.hasText(paymentType)) {
            throw new BizException("支付方式不能为空");
        }
    }

    private String buildCreateOrderLockKey(Long productId, String paymentType) {
        return "payment:order:create:" + productId + ":" + paymentType;
    }
}
