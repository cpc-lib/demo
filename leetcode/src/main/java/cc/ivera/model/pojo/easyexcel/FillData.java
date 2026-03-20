package cc.ivera.model.pojo.easyexcel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class FillData {
    private String name;
    private double number;
    // lombok 会生成getter/setter方法
}