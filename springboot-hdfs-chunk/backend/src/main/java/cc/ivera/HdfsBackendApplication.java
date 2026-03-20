package cc.ivera;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class HdfsBackendApplication {

    public static void main(String[] args) {
        System.setProperty("hadoop.home.dir", "D:\\hadoop_dummy");
        System.setProperty("hadoop.home.ignore.winutils", "true");
        new File("D:\\hadoop_dummy\\bin").mkdirs();
        SpringApplication.run(HdfsBackendApplication.class, args);
    }
}
