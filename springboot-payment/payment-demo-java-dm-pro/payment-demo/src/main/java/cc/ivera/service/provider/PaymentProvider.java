package cc.ivera.service.provider;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.service.refund.RefundStatusSyncResult;

import java.util.Map;

/**
 * 统一支付提供商接口 — Strategy 模式。
 *
 * 将 AliPayService 和 WxPayOrderFacade/WxPayRefundFacade 的公共语义
 * 收敛到一个接口，消除调用方对具体 Provider 类型的硬编码依赖。
 */
public interface PaymentProvider {

    /**
     * 创建支付（下单）。
     *
     * @param productId    商品 ID
     * @param paymentAppId 支付应用 ID，可为 null
     * @return 支付入口数据（支付宝返回 HTML form，微信返回 code_url + orderNo）
     */
    Object createPayment(Long productId, Long paymentAppId);

    /**
     * 处理支付异步通知。
     *
     * @param params 通知参数
     */
    void processPaymentNotification(Map<String, ?> params);

    /**
     * 取消订单。
     *
     * @param orderNo 订单号
     */
    void cancelOrder(String orderNo);

    /**
     * 查询订单状态（返回渠道原始响应）。
     *
     * @param orderNo 订单号
     * @return 渠道原始响应字符串
     */
    String queryOrder(String orderNo);

    /**
     * 核实订单状态并同步本地状态（关单/补单）。
     *
     * @param orderNo 订单号
     */
    void checkOrderStatus(String orderNo);

    /**
     * 执行退款。
     *
     * @param refundInfo 退款单信息
     */
    void executeRefund(RefundInfo refundInfo);

    /**
     * 查询退款状态。
     *
     * @param refundNo 退款单号
     * @return 渠道原始响应字符串
     */
    String queryRefund(String refundNo);

    /**
     * 查询退款状态用于同步。
     *
     * @param refundNo 退款单号
     * @return 退款状态同步结果
     */
    RefundStatusSyncResult queryRefundStatusForSync(String refundNo);

    /**
     * 获取此 Provider 支持的支付类型。
     *
     * @return 支付类型标识（WXPAY / ALIPAY）
     */
    String getPaymentType();
}
