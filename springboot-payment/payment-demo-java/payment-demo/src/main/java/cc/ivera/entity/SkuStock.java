package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_sku_stock")
public class SkuStock {

    @TableId(value = "sku_id", type = IdType.INPUT)
    private Long skuId;

    private Integer availableStock;

    private Integer lockedStock;

    private Long version;

    private Date createTime;

    private Date updateTime;
}

