package cc.ivera.sql;

import cc.ivera.bean.WaterSensor;

public class HashDemo {
    public static void main(String[] args) {
        WaterSensor s1 = new WaterSensor("s1", 1L, 1);

        /**
         * hash=s[0]×31^(n−1)+s[1]×31^(n−2)+⋯+s[n−1]
         *
         * unicode
         *
         * 115*31+49*1
         */
        System.out.println(s1.getId().hashCode());
    }
}
