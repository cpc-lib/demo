package cc.ivera.userdemo;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
    "APP_INIT_HBASE=false",
    "APP_INIT_ES=false"
})
class UserDemoApplicationTests {
  @Test void contextLoads() {}
}
