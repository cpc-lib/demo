package cc.ivera;

import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@SpringBootTest
class MinioUploadFileApplicationTests {

    @Test
    void contextLoads() {
    }

    @Value(value = "${minio.endpoint}")
    private String endpoint;
    @Value(value = "${minio.accesskey}")
    private String accesskey;
    @Value(value = "${minio.secretkey}")
    private String secretkey;


    @Test
    public void getPreviewFileUrl() throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        MinioClient minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accesskey, secretkey)
                .build();
       minioClient.statObject(
                StatObjectArgs.builder().bucket("video").object("jjj.mp4").build());
        minioClient.removeObject(
                RemoveObjectArgs.builder().bucket("video").object("16823477131613__43_Pro.mp4").build());
//        try {
//            minioClient.removeObject("video","16823462732");
//        } catch (Exception e) {
//            System.out.println("删除失败");
//        }
//        System.out.println("删除成功");
    }

}
