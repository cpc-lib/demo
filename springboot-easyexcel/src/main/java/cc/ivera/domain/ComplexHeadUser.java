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
public class ComplexHeadUser {
    @ExcelProperty(value = {"group1", "用户编号"}, index = 0)
    private Integer userId;
    @ExcelProperty(value = {"group1", "姓名"}, index = 1)
    private String userName;
    @ExcelProperty(value = {"group2", "入职时间"}, index = 2)
    private Date hireDate;
    // lombok 会生成getter/setter方法
}
