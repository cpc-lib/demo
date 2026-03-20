package com.example.orderdemo;

import com.example.orderdemo.infrastructure.es.EsIndexInitializer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;

@SpringBootApplication
public class OrderDemoApplication {

  private final EsIndexInitializer esIndexInitializer;

  public OrderDemoApplication(EsIndexInitializer esIndexInitializer) {
    this.esIndexInitializer = esIndexInitializer;
  }

  public static void main(String[] args) {
    SpringApplication.run(OrderDemoApplication.class, args);
  }

  @EventListener(ApplicationReadyEvent.class)
  public void onReady() throws Exception {
    esIndexInitializer.ensureIndex();
  }
}
