package cc.ivera;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;

import java.io.IOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class AlipayTests {

    @Test
    public void testAlipayConfig() throws IOException {
        Properties properties = PropertiesLoaderUtils.loadProperties(
                new ClassPathResource("alipay-sandbox.properties"));

        assertThat(properties.getProperty("alipay.appId")).isNotBlank();
        assertThat(properties.getProperty("alipay.gatewayUrl")).startsWith("https://");
        assertThat(properties.getProperty("alipay.merchantPrivateKey")).isNotBlank();
        assertThat(properties.getProperty("alipay.alipayPublicKey")).isNotBlank();
    }
}
