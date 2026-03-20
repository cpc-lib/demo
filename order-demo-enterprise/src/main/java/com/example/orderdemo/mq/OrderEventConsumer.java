package com.example.orderdemo.mq;

import com.example.orderdemo.domain.event.OrderEventMessage;
import com.example.orderdemo.domain.event.OrderEsDocument;
import com.example.orderdemo.infrastructure.es.OrderEsRepository;
import com.example.orderdemo.infrastructure.json.Jsons;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Slf4j
@Component
public class OrderEventConsumer {

  private final OrderEsRepository orderEsRepository;

  public OrderEventConsumer(OrderEsRepository orderEsRepository) {
    this.orderEsRepository = orderEsRepository;
  }

  @KafkaListener(topics = "${app.kafka.topic}", groupId = "order-es-writer")
  public void onMessage(String value, Acknowledgment ack) {
    try {
      OrderEventMessage msg = Jsons.fromJson(value, OrderEventMessage.class);

      OrderEsDocument doc = new OrderEsDocument();
      doc.setOrderId(String.valueOf(msg.getOrderId()));
      doc.setUserId(String.valueOf(msg.getUserId()));
      doc.setStatus(msg.getStatus());
      doc.setTotalAmount(msg.getTotalAmount());
      doc.setCreatedAt(msg.getCreatedAt());
      doc.setUpdatedAt(msg.getUpdatedAt());
      doc.setItems(msg.getItems().stream().map(x -> {
        OrderEsDocument.Item i = new OrderEsDocument.Item();
        i.setSkuId(String.valueOf(x.getSkuId()));
        i.setTitle(x.getTitle());
        i.setPrice(x.getPrice());
        i.setQuantity(x.getQuantity());
        return i;
      }).collect(Collectors.toList()));

      orderEsRepository.upsert(doc);
      ack.acknowledge();
    } catch (Exception e) {
      // 不 ack -> 让 Kafka 重试（配合 DLQ/重试策略更完整）
      log.error("consume failed, will retry: {}", e.getMessage(), e);
    }
  }
}
