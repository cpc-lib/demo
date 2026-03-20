package cc.ivera.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class GoodsVo implements Serializable {


    @JsonProperty("id")
    private String goodsId;

    @JsonProperty("goods_name")
    private String goodsName;

    @JsonProperty("goods_price")
    private BigDecimal goodsPrice;


    @JsonProperty("tags")
    private List<String> tags;
}
