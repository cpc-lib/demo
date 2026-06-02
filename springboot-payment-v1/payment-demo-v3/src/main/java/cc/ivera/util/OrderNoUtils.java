package cc.ivera.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

/**
 * 订单号工具类
 *
 * @author qy
 * @since 1.0
 */
public class OrderNoUtils {

    private static final Random RANDOM = new Random();
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMddHHmmss");

    /**
     * 获取订单编号
     *
     * @return
     */
    public static String getOrderNo() {
        return "ORD" + getNo();
    }

    /**
     * 获取退款单编号
     *
     * @return
     */
    public static String getRefundNo() {
        return "RFD" + getNo();
    }

    /**
     * 获取编号
     *
     * @return
     */
    public static String getNo() {
        String timestamp = DATE_FORMAT.format(new Date());
        int random = RANDOM.nextInt(10000);
        return timestamp + String.format("%04d", random);
    }

}
