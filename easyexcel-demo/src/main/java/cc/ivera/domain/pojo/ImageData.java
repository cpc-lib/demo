package cc.ivera.domain.pojo;

import cn.idev.excel.annotation.ExcelIgnore;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.converters.string.StringImageConverter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {

    //使用抽象文件表示一个图片
    @ExcelProperty(value = "File类型")
    private File file;
    // 使用输入流保存一个图片(有问题图片不显示)
    //@ExcelProperty(value = "InputStream类型")
    @ExcelIgnore
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
