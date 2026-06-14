package cc.ivera.domain.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BasePropertyType {
    private String property_type_id;//主键
    private String property_type_code;//物业分类编码
    private String property_type_name;//物业分类
    private Integer type_level;//等级
}