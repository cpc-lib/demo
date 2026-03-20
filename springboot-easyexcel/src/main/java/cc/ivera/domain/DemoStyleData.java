package cc.ivera.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.*;
import com.alibaba.excel.enums.poi.FillPatternTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.poi.ss.usermodel.FillPatternType;

import java.util.Date;


@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@HeadRowHeight(value = 30) // 头部行高
@ContentRowHeight(value = 25) // 内容行高
@ColumnWidth(value = 20) // 列宽
// 头背景设置成红色 IndexedColors.RED.getIndex()
@HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 10)
// 头字体设置成20, 字体默认宋体
@HeadFontStyle(fontName = "宋体", fontHeightInPoints = 20)
// 内容的背景设置成绿色  IndexedColors.GREEN.getIndex()
@ContentStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 17)
// 内容字体设置成20, 字体默认宋体
@ContentFontStyle(fontName = "宋体", fontHeightInPoints = 20)
public class DemoStyleData {

    // 字符串的头背景设置成粉红 IndexedColors.PINK.getIndex()
    @HeadStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 14)
    // 字符串的头字体设置成20
    @HeadFontStyle(fontHeightInPoints = 30)
    // 字符串的内容背景设置成天蓝 IndexedColors.SKY_BLUE.getIndex()
    @ContentStyle(fillPatternType = FillPatternTypeEnum.SOLID_FOREGROUND, fillForegroundColor = 40)
    // 字符串的内容字体设置成20,默认宋体
    @ContentFontStyle(fontName = "宋体", fontHeightInPoints = 20)
    @ExcelProperty(value = "字符串标题")
    private String string;
    @ExcelProperty(value = "日期标题")
    private Date date;
    @ExcelProperty(value = "数字标题")
    private Double doubleData;
    // lombok 会生成getter/setter方法
}
