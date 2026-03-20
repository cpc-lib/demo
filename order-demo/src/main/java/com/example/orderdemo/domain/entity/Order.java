package com.example.orderdemo.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("t_order")
public class Order {

  @TableId(type = IdType.AUTO)
  private Long id;

  private Long userId;
  private String orderNo;
  private BigDecimal totalAmount;
  private String status;
  private String remark;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
