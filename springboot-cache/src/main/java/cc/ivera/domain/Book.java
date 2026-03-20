package cc.ivera.domain;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Book implements Serializable {
    private Long id;
    private String name;
    @JsonSerialize(using= ToStringSerializer.class)
    private BigDecimal price;
    private String description;
}
