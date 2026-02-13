package cc.ivera.studentdemo;

import cc.ivera.studentdemo.hbase.HBaseTableInitializer;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class StudentHBaseCrudDemoApplication implements CommandLineRunner {

  private final HBaseTableInitializer initializer;

  public static void main(String[] args) {
    SpringApplication.run(StudentHBaseCrudDemoApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
    initializer.ensureTable();
  }



}
