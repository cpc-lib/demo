package cc.ivera.userdemo;

import cc.ivera.userdemo.init.SchemaInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@RequiredArgsConstructor
public class UserDemoApplication implements CommandLineRunner {

  private final SchemaInitializer schemaInitializer;

  public static void main(String[] args) {
    SpringApplication.run(UserDemoApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    schemaInitializer.initHBase();
    schemaInitializer.initEs();
  }
}
