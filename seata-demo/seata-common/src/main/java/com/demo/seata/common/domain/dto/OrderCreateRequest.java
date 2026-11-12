package com.demo.seata.common.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 订单创建请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCreateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务订单号(客户端传递，用于全链路防重与幂等保证)
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
     * 是否模拟异常以触发分布式事务回滚 (用于测试)
     */
    private Boolean mockException;
}
