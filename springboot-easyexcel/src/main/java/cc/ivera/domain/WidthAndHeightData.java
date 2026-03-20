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
@HeadRowHeight(value = 30) // 头部行高
@ContentRowHeight(value = 25) // 内容行高
@ColumnWidth(value = 20) // 列宽
public class WidthAndHeightData {
    @ExcelProperty(value = "字符串标题")
    private String string;
    @ExcelProperty(value = "日期标题")
    private Date date;
    @ExcelProperty(value = "数字标题")
    @ColumnWidth(value = 25)
    private Double doubleData;
    // lombok 会生成getter/setter方法
}
