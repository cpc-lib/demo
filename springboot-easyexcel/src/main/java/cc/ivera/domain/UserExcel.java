package cc.ivera.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@HeadRowHeight(value = 30)
@ContentRowHeight(value = 25)
@ColumnWidth(value = 30)
public class UserExcel {
    @ExcelProperty(value = "用户编号")
    private Integer userId;
    @ExcelProperty(value = "姓名")
    private String username;
    @ExcelProperty(value = "性别")
    private String gender;
    @ExcelProperty(value = "工资")
    private Double salary;
    @ExcelProperty(value = "入职时间")
    private Date hireDate;
}
