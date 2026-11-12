package com.demo.seata.storage.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * SAGA 模式库存正向操作与补偿幂等记录实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("storage_saga_record")
public class StorageSagaRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 业务唯一Key (例如 orderNo)
     */
    @TableId
    private String businessKey;

    /**
     * 商品编码
     */
    private String commodityCode;

    /**
     * 扣减数量
     */
    private Integer count;

    /**
     * 正向状态: 0-未执行, 1-成功, 2-失败
     */
    private Integer forwardStatus;

    /**
     * 补偿状态: 0-未补偿, 1-已补偿
     */
    private Integer compensateStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
