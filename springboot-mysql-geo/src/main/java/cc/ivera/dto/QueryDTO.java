package cc.ivera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class QueryDTO implements Serializable {
    private BigDecimal longitude;
    private BigDecimal latitude;
    private BigDecimal distance;
}
