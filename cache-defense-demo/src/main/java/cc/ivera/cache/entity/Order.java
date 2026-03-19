
package cc.ivera.cache.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("order_info")
public class Order {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
}
