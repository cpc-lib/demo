package com.demo.seata.common.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 通用业务幂等记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("idempotent_record")
public class IdempotentRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 幂等唯一键(例如 orderNo, token, requestId 等)
     */
    private String idempotentKey;

    /**
     * 业务类型
     */
    private String bizType;

    /**
     * 状态: PROCESSING, SUCCESS, FAILED
     */
    private String status;

    /**
     * 响应缓存数据(JSON格式)
     */
    private String responseData;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
