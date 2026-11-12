package com.demo.seata.storage.entity;

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
 * 库存实体
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("storage_tbl")
public class Storage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 商品编码
     */
    private String commodityCode;

    /**
     * 商品名称
     */
    private String name;

    /**
     * 总库存
     */
    private Integer totalCount;

    /**
     * 可用库存
     */
    private Integer availableCount;

    /**
     * 冻结库存 (TCC 模式使用)
     */
    private Integer frozenCount;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
