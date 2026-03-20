package cc.ivera.common.mybatis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Geometry implements Serializable {
    /*
    经度
     */
    private BigDecimal longitude;
    /*
    纬度
     */
    private BigDecimal latitude;
}
