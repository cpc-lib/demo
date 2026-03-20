package cc.ivera.util;


import org.apache.commons.io.FileUtils;
import org.lionsoul.ip2region.xdb.Searcher;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.text.MessageFormat;
import java.util.Objects;

public class AddressUtil {

    /**
     * 当前记录地址的本地DB
     */
    private static final String TEMP_FILE_DIR = "/home/admin/app/";

    /**
     * 根据IP地址查询登录来源
     *
     * @param ip
     * @return
     */
    public static String getCityInfo(String ip) {
        try {
            // 获取当前记录地址位置的文件
            File file = ResourceUtils.getFile("classpath:ip2region/ip2region.xdb");
            String dbPath = file.getAbsolutePath();
            //如果当前文件不存在，则从缓存中复制一份
            if (!file.exists()) {
                dbPath = TEMP_FILE_DIR + "ip.db";
                System.out.println(MessageFormat.format("当前目录为:[{0}]", dbPath));
                file = new File(dbPath);
                FileUtils.copyInputStreamToFile(Objects.requireNonNull(AddressUtil.class.getClassLoader().getResourceAsStream("classpath:ip2region/ip2region.xdb")), file);
            }
            //创建查询对象
            Searcher searcher = Searcher.newWithFileOnly(dbPath);
            //开始查询
            return searcher.search(ip);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //默认返回空字符串
        return "";
    }

}
