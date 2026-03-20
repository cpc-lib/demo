package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Good extends BaseEntity {

    @TableId("good_id")
    private String goodId;

    private String goodName;

    private Long total;

    /**
     * 初始化版本为0
     */
    @Version
    private Long version = 0L;

}
