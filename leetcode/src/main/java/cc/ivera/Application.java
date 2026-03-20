package cc.ivera;

import com.github.hiwepy.ip2region.spring.boot.IP2regionTemplate;
import com.github.hiwepy.ip2region.spring.boot.ext.RegionAddress;
import com.github.hiwepy.ip2region.spring.boot.ext.RegionEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import javax.annotation.PostConstruct;
import java.io.IOException;

@SpringBootApplication
@EnableScheduling
public class Application {
    @Autowired
    IP2regionTemplate template;

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    //文件大小配置 采用 配置文件进行配置 也可在yml进行参数配置
//    @Bean(name = "multipartResolver")
//    public CommonsMultipartResolver multipartResolver() {
//        CommonsMultipartResolver multipartResolver = new
//                CommonsMultipartResolver();
//        multipartResolver.setMaxUploadSize(1000000000);
//        return multipartResolver;
//    }

    @PostConstruct
    public void test() throws IOException {

        System.out.println(template.btreeSearch("61.94.43.82"));
        System.out.println(template.binarySearch("61.94.43.82"));
        System.out.println(template.memorySearch("61.94.43.82"));
        System.out.println(template.binarySearch("127.0.0.1"));

        // 根据IP获取对应国家
        System.out.println(template.getCountryByIp("127.0.0.1"));

        // 根据IP获取对应地区
        System.out.println(template.getRegion("114.124.146.103") + "----?");

        // 根据IP获取对应地区详细信息对象
        RegionAddress adress1 = template.getRegionAddress("113.210.53.80");
        System.out.println(adress1);
        System.out.println(RegionEnum.getByRegionAddress(adress1));

        // 根据IP获取对应地区枚举
        RegionEnum regionEnum = template.getRegionByIp("102.42.140.162");
        System.out.println(regionEnum);

    }
}
