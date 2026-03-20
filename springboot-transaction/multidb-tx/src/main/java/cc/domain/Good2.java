package cc.ivera.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Good2 implements Serializable {

    private Integer goods_count;

    private String goods_img;

    private String goods_name;

    private Integer goods_price;

    private Boolean good_state;

    private Integer id;

}
