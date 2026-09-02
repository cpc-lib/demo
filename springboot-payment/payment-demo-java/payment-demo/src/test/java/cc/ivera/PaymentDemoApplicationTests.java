package cc.ivera;

import cc.ivera.config.WxPayConfig;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.security.PrivateKey;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentDemoApplicationTests {

    /**
     * 获取商户的私钥
     */
    @Test
    void testGetPrivateKey() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(new ClassPathResource("wxpay.properties"));
        String privateKeyPath = properties.getProperty("wxpay.private-key-path");
        WxPayConfig wxPayConfig = new WxPayConfig();

        assertThat(privateKeyPath).isNotBlank();
        assertThat(new ClassPathResource(privateKeyPath).exists()).isTrue();

        PrivateKey privateKey = wxPayConfig.getPrivateKey(privateKeyPath);

        assertThat(privateKey).isNotNull();
        assertThat(privateKey.getAlgorithm()).isEqualTo("RSA");
        assertThat(privateKey.getEncoded()).isNotEmpty();
    }

}
