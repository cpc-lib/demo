package cc.ivera.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.converters.string.StringImageConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.io.File;
import java.io.InputStream;
import java.net.URL;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@ContentRowHeight(value = 100) // 内容行高
@ColumnWidth(value = 30) // 列宽
public class ImageData {

    //使用抽象文件表示一个图片
    @ExcelProperty(value = "File类型")
    private File file;
    // 使用输入流保存一个图片
    @ExcelProperty(value = "InputStream类型")
    private InputStream inputStream;
    // 当使用String类型保存一个图片的时候需要使用StringImageConverter转换器
    @ExcelProperty(value = "String类型", converter = StringImageConverter.class)
    private String str;
    // 使用二进制数据保存为一个图片
    @ExcelProperty(value = "二进制数据(字节)")
    private byte[] byteArr;
    // 使用网络链接保存为一个图片
    @ExcelProperty(value = "网络图片")
    private URL url;
}
