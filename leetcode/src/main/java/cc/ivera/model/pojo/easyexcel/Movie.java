package cc.ivera.model.pojo.easyexcel;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Movie implements Serializable {
    @ExcelProperty(value = "标题", index = 0)
    private String title;
    @ExcelProperty(value = "类型", index = 1)
    private String fullText;
    @ExcelProperty(value = "评价", index = 2)
    private String quote;
    @ExcelProperty(value = "评分", index = 3)
    private String score;
}
