package cc.ivera.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationVo implements Serializable {
    private String id;
    private String name;
    private String distance;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
