package com.example.orderdemo.service;

import com.example.orderdemo.domain.dto.OrderDetailDTO;
import com.example.orderdemo.domain.dto.OrderItemDTO;
import com.example.orderdemo.domain.dto.OrderSearchRequest;
import com.example.orderdemo.domain.dto.OrderSearchResponse;
import com.example.orderdemo.domain.event.OrderEsDocument;
import com.example.orderdemo.infrastructure.es.OrderEsRepository;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@Service
public class OrderSearchService {

  private final OrderEsRepository orderEsRepository;

  public OrderSearchService(OrderEsRepository orderEsRepository) {
    this.orderEsRepository = orderEsRepository;
  }

  public OrderSearchResponse search(OrderSearchRequest req) {
    try {
      var r = orderEsRepository.search(req.getUserId(), req.getStatus(),
              req.getMinTotalAmount(), req.getMaxTotalAmount(),
              req.getCreatedAtFrom(),req.getCreatedAtTo(),
              req.getPage(), req.getSize());

      OrderSearchResponse resp = new OrderSearchResponse();
      resp.setTotal(r.total);
      resp.setOrders(r.docs.stream().map(this::toDto).collect(Collectors.toList()));
      return resp;
    } catch (Exception e) {
      throw new IllegalStateException("search es failed: " + e.getMessage(), e);
    }
  }

  private OrderDetailDTO toDto(OrderEsDocument d) {
    OrderDetailDTO dto = new OrderDetailDTO();
    dto.setOrderId(Long.parseLong(d.getOrderId()));
    dto.setUserId(Long.parseLong(d.getUserId()));
    dto.setStatus(d.getStatus());
    dto.setTotalAmount(d.getTotalAmount());
    dto.setCreatedAt(d.getCreatedAt());
    dto.setUpdatedAt(d.getUpdatedAt());
    dto.setItems(d.getItems().stream().map(x -> {
      OrderItemDTO i = new OrderItemDTO();
      i.setSkuId(Long.parseLong(x.getSkuId()));
      i.setTitle(x.getTitle());
      i.setPrice(x.getPrice());
      i.setQuantity(x.getQuantity());
      return i;
    }).collect(Collectors.toList()));
    return dto;
  }
}
