package cc.ivera;

import cc.ivera.config.WxPayConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.security.PrivateKey;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class PaymentDemoApplicationTests {

    private final WxPayConfig wxPayConfig;

    private final CloseableHttpClient wxPayClient;

    @Autowired
    public PaymentDemoApplicationTests( WxPayConfig wxPayConfig,CloseableHttpClient wxPayClient){
        this.wxPayConfig=wxPayConfig;
        this.wxPayClient=wxPayClient;
    }


    /**
     * 获取商户的私钥
     */
    @Test
    void testGetPrivateKey() {

        //获取私钥路径
        String privateKeyPath = wxPayConfig.getPrivateKeyPath();

        //获取私钥
        PrivateKey privateKey = wxPayConfig.getPrivateKey(privateKeyPath);

        System.out.println(privateKey);

    }

}
