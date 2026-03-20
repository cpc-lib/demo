package cc.ivera.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.RandomUtil;

import static cn.hutool.core.util.IdUtil.getSnowflake;

/**
 * ID生成器工具类
 *
 * @author ruoyi
 */
public class IdUtil {


    public static Long WORKER_ID = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) % 32L;

    public static Long DATA_CENTER_ID = RandomUtil.randomLong(32L);

    public static Snowflake SNOW_FLAKE;

    static {
        SNOW_FLAKE = getSnowflake(WORKER_ID, DATA_CENTER_ID);
    }

    public IdUtil() {
    }

    /**
     * 获取随机UUID
     *
     * @return 随机UUID
     */
    public static String randomUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String simpleUUID() {
        return UUID.randomUUID().toString(true);
    }

    /**
     * 获取随机UUID，使用性能更好的ThreadLocalRandom生成UUID
     *
     * @return 随机UUID
     */
    public static String fastUUID() {
        return UUID.fastUUID().toString();
    }

    /**
     * 简化的UUID，去掉了横线，使用性能更好的ThreadLocalRandom生成UUID
     *
     * @return 简化的UUID，去掉了横线
     */
    public static String fastSimpleUUID() {
        return UUID.fastUUID().toString(true);
    }

    public static Long getId() {
        return SNOW_FLAKE.nextId();
    }

    public static String getStrId() {
        return SNOW_FLAKE.nextIdStr();
    }


}
