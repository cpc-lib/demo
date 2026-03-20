package cc.ivera.model.pojo.easyexcel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.write.style.ColumnWidth;
import cn.idev.excel.annotation.write.style.ContentRowHeight;
import cn.idev.excel.annotation.write.style.HeadRowHeight;
import cn.idev.excel.annotation.write.style.OnceAbsoluteMerge;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@HeadRowHeight(value = 25) // 头部行高
@ContentRowHeight(value = 20) // 内容行高
@ColumnWidth(value = 20) // 列宽
/**
 * @OnceAbsoluteMerge 指定从哪一行/列开始,哪一行/列结束,进行单元格合并
 * firstRowIndex 起始行索引,从0开始
 * lastRowIndex 结束行索引
 * firstColumnIndex 起始列索引,从0开始
 * lastColumnIndex 结束列索引
 */
// 例如: 第2-3行,2-3列进行合并
@OnceAbsoluteMerge(
        firstRowIndex = 1,
        lastRowIndex = 2,
        firstColumnIndex = 1,
        lastColumnIndex = 2)
public class MergeData {

    // 每隔两行合并一次(竖着合并单元格)
//    @ContentLoopMerge(eachRow = 2)
    @ExcelProperty(value = "字符串标题")
    private String string;
    @ExcelProperty(value = "日期标题")
    private Date date;
    @ExcelProperty(value = "数字标题")
    private Double doubleData;
    // lombok 会生成getter/setter方法
}
