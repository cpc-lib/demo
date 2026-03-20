package cc.ivera;

import com.alicp.jetcache.anno.config.EnableCreateCacheAnnotation;
import com.alicp.jetcache.anno.config.EnableMethodCache;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
//cache1.0
//@EnableCaching

//cache2.0
@EnableCreateCacheAnnotation
@EnableMethodCache(basePackages = "cc.ivera")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class,args);
    }
}
