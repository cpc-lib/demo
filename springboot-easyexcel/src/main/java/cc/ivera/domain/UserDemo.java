package cc.ivera.domain;


import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.format.NumberFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class UserDemo {

    @ExcelProperty(value = "用户编号", index = 0)
    private Integer userId;
    @ExcelProperty(value = "姓名", index = 1)
    private String userName;
    @ExcelProperty(value = "性别", index = 3)
    private String gender;
    @ExcelProperty(value = "工资", index = 4)
    @NumberFormat(value="#####.#") // 数字格式化,保留1位小数
    private Double salary;
    @ExcelProperty(value = "入职时间", index = 2)
    @DateTimeFormat(value = "yyyy年MM月dd日 HH时mm分ss秒") // 日期格式化
    private Date hireDate;
    // lombok 会生成getter/setter方法
}
