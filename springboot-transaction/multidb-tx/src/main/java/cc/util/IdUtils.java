package cc.ivera.util;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.net.NetUtil;
import cn.hutool.core.util.RandomUtil;

/**
 * 主键生成工具类
 * 采用twitter的雪花算法
 */
public class IdUtils extends cn.hutool.core.util.IdUtil {

    public static Long WORKER_ID = NetUtil.ipv4ToLong(NetUtil.getLocalhostStr()) % 32L;
    public static Long DATA_CENTER_ID = RandomUtil.randomLong(32L);
    public static Snowflake SNOW_FLAKE;

    public IdUtils() {
    }

    public static Long getId() {
        return SNOW_FLAKE.nextId();
    }

    public static String getStrId() {
        return SNOW_FLAKE.nextIdStr();
    }

    static {
        SNOW_FLAKE = getSnowflake(WORKER_ID, DATA_CENTER_ID);
    }
}
