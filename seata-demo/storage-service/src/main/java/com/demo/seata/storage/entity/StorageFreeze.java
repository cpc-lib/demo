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
 * TCC 模式库存预留冻结实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("storage_freeze_tbl")
public class StorageFreeze implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 全局事务 XID
     */
    @TableId
    private String xid;

    /**
     * 分支事务 ID
     */
    private Long branchId;

    /**
     * 商品编码
     */
    private String commodityCode;

    /**
     * 冻结数量
     */
    private Integer count;

    /**
     * 状态: 1-TRY(预留), 2-CONFIRM(已确认扣减), 3-CANCEL(已回滚释放)
     */
    private Integer status;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
