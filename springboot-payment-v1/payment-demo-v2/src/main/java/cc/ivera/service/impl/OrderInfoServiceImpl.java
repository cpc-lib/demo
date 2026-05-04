package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.Product;
import cc.ivera.exception.BizException;
import cc.ivera.enums.OrderStatus;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    @Resource
    private ProductMapper productMapper;

    @Resource
    private OrderCloseMessageService orderCloseMessageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderInfo createOrReuseOrder(Long productId, String paymentType) {
        if (productId == null) {
            throw new BizException("商品ID不能为空");
        }
        if (paymentType == null || paymentType.trim().isEmpty()) {
            throw new BizException("支付方式不能为空");
        }

        // 查找已存在但未支付的订单，避免重复创建
        OrderInfo orderInfo = this.getNoPayOrderByProductId(productId, paymentType);
        if (orderInfo != null) {
            return orderInfo;
        }

        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new BizException("商品不存在");
        }

        orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);
        baseMapper.insert(orderInfo);

        orderCloseMessageService.sendCloseOrderMessage(orderInfo.getOrderNo(), paymentType);
        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        baseMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public List<OrderInfo> listOrderByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>().orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        log.info("更新订单状态 ===> {}", orderStatus.getType());

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());

        baseMapper.update(orderInfo, queryWrapper);
    }

    @Override
    public boolean updateStatusByOrderNoIfStatus(String orderNo, OrderStatus currentStatus, OrderStatus targetStatus) {
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
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo);
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNoForUpdate(String orderNo) {
        return baseMapper.selectByOrderNoForUpdate(orderNo);
    }

    /**
     * 根据商品id查询未支付订单，防止重复创建订单对象
     */
    private OrderInfo getNoPayOrderByProductId(Long productId, String paymentType) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("product_id", productId);
        queryWrapper.eq("order_status", OrderStatus.NOTPAY.getType());
        queryWrapper.eq("payment_type", paymentType);
        return baseMapper.selectOne(queryWrapper);
    }
}
