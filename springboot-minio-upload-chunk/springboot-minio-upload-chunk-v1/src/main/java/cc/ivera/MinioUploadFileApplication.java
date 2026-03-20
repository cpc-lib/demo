package cc.ivera;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MinioUploadFileApplication {
    private static final Logger logger = LoggerFactory.getLogger(MinioUploadFileApplication.class);
    public static void main(String[] args) {

        SpringApplication.run(MinioUploadFileApplication.class, args);
        logger.info("\n\n\n博主博客地址：https://blog.csdn.net/weixin_51603038\n\n\n");
    }

}
