package cc.ivera;

import cn.hutool.core.util.IdUtil;

public class Test {
    public static void main(String[] args) {
        System.out.println(IdUtil.getSnowflake().nextId());
    }
}
