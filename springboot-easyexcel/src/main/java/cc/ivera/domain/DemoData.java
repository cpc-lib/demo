package cc.ivera.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class DemoData {
    // 根据Excel中指定列名或列的索引读取
    @ExcelProperty(value = "字符串标题", index = 0)
    private String name;
    @ExcelProperty(value = "日期标题", index = 1)
    private Date hireDate;
    @ExcelProperty(value = "数字标题", index = 2)
    private Double salary;
    // lombok 会生成getter/setter方法
}
