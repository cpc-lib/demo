package com.example.demo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
  private static final Logger log = LoggerFactory.getLogger(TestController.class);

  @GetMapping("/trace")
  public String trace(){
    log.info("{},I love you 3,000","Yes");
    return "OK";
  }
}
