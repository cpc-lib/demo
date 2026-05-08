package cc.ivera.util;

import java.util.UUID;

/**
 * 订单号工具类
 *
 * @author qy
 * @since 1.0
 */
public class OrderNoUtils {

    /**
     * 获取订单编号
     *
     * @return
     */
    public static String getOrderNo() {
        return "ORDER_" + getNo();
    }

    /**
     * 获取退款单编号
     *
     * @return
     */
    public static String getRefundNo() {
        return "REFUND_" + getNo();
    }

    /**
     * 获取编号
     *
     * @return
     */
    public static String getNo() {
        return UUID.randomUUID().toString().replace("-", "").toUpperCase();
    }

}
