package cc.ivera.model.pojo;

import com.deepoove.poi.data.PictureRenderData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PoiUser {
    private String name;
    private Integer age;
    private PictureRenderData profile;
    private BigDecimal salary;
}
