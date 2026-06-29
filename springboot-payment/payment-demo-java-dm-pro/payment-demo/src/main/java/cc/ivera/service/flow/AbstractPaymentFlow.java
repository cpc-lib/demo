package cc.ivera.service.flow;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.state.OrderStateMachine;
import cc.ivera.util.OrderNoUtils;

/**
 * 支付流程模板 — Template Method 模式。
 *
 * 定义支付下单的骨架流程，子类实现渠道特定的 API 调用。
 *
 * @param <R> 支付结果类型
 */
public abstract class AbstractPaymentFlow<R> {

    protected final OrderInfoService orderInfoService;
    protected final PaymentConfigLoader paymentConfigLoader;
    protected final DistributedLockTemplate distributedLockTemplate;

    protected AbstractPaymentFlow(OrderInfoService orderInfoService,
                                  PaymentConfigLoader paymentConfigLoader,
                                  DistributedLockTemplate distributedLockTemplate) {
        this.orderInfoService = orderInfoService;
        this.paymentConfigLoader = paymentConfigLoader;
        this.distributedLockTemplate = distributedLockTemplate;
    }

    /**
     * 模板方法 — 支付下单骨架。
     */
    public final R execute(Long productId, Long paymentAppId) {
        String lockKey = buildLockKey(productId, paymentAppId);
        return distributedLockTemplate.execute(lockKey, getLockWaitMs(), getLockLeaseMs(), () -> {
            // 1. 前置校验
            preValidate(productId, paymentAppId);

            // 2. 解析支付配置
            PaymentAppConfig payConfig = resolvePaymentConfig(paymentAppId);

            // 3. 创建或复用订单
            OrderInfo orderInfo = orderInfoService.createOrReuseOrder(
                    productId,
                    getPaymentType(),
                    payConfig.getAppId(),
                    getChannelCode());

            if (orderInfo == null) {
                throw new BizException("订单创建失败");
            }

            // 4. 检查是否已有支付入口（复用）
            R cachedResult = checkCachedResult(orderInfo);
            if (cachedResult != null) {
                return cachedResult;
            }

            // 5. 调用渠道 API 创建支付
            R result = callPaymentApi(payConfig, orderInfo);

            // 6. 保存支付入口信息
            savePaymentEntry(orderInfo, result);

            // 7. 后置处理
            postProcess(orderInfo);

            return result;
        });
    }

    /** 构建分布式锁 key */
    protected abstract String buildLockKey(Long productId, Long paymentAppId);

    /** 获取锁等待时间 */
    protected long getLockWaitMs() { return 3000L; }

    /** 获取锁租约时间 */
    protected long getLockLeaseMs() { return -1L; }

    /** 获取支付类型 */
    protected abstract String getPaymentType();

    /** 获取渠道编码 */
    protected abstract String getChannelCode();

    /** 前置校验 */
    protected void preValidate(Long productId, Long paymentAppId) {
        if (productId == null) {
            throw new BizException("商品ID不能为空");
        }
    }

    /** 解析支付配置 */
    protected abstract PaymentAppConfig resolvePaymentConfig(Long paymentAppId);

    /** 检查缓存结果（如已有二维码） */
    protected abstract R checkCachedResult(OrderInfo orderInfo);

    /** 调用渠道支付 API */
    protected abstract R callPaymentApi(PaymentAppConfig payConfig, OrderInfo orderInfo);

    /** 保存支付入口信息 */
    protected abstract void savePaymentEntry(OrderInfo orderInfo, R result);

    /** 后置处理 */
    protected void postProcess(OrderInfo orderInfo) {
        // 子类可覆盖
    }
}
