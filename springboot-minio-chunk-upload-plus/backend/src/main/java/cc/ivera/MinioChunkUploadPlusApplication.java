package cc.ivera;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.chunk.mapper")
public class MinioChunkUploadPlusApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinioChunkUploadPlusApplication.class, args);
    }
}
