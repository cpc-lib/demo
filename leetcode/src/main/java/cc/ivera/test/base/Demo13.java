package cc.ivera.test.base;


import org.apache.commons.lang3.StringUtils;


public class Demo13 {
    public static void main(String[] args) {
        String path = "1704384627271786496;1712669776420003840;1712669776721993728;";
        String[] split = path.split(";");
        String join = StringUtils.join(split, ",");
        System.out.println(join);
    }
}
