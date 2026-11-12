package com.demo.seata.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("order_info")
public class OrderInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 业务订单号 (全链路业务幂等唯一索引)
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 商品编码
     */
    private String commodityCode;

    /**
     * 购买数量
     */
    private Integer count;

    /**
     * 订单金额
     */
    private BigDecimal money;

    /**
     * 状态: INIT, SUCCESS, FAILED, CANCELLED
     */
    private String status;

    /**
     * 事务模式: AT, TCC, SAGA, XA
     */
    private String mode;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
