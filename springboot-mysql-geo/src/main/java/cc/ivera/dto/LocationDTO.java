package cc.ivera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LocationDTO implements Serializable {
    private String id;
    private String name;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
