package cc.ivera;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
@Slf4j
public class AlipayTests {

    private final Environment config;

    @Autowired
    public AlipayTests(Environment config) {
        this.config = config;
    }

    @Test
    public void testAlipayConfig() {
        log.info(config.getProperty("alipay.appId"));
    }
}
