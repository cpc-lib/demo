package cc.ivera.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class EsGoodsVo implements Serializable {

    @JsonProperty("id")
    private String id;

    @JsonProperty("goods_img")
    private String goodsImg;

    @JsonProperty("goods_name")
    private String goodsName;

    @JsonProperty("goods_price")
    private BigDecimal goodsPrice;

    @JsonProperty("goods_count")
    private Integer goodsCount;

    @JsonProperty("goods_state")
    private Boolean goodsState;

}
