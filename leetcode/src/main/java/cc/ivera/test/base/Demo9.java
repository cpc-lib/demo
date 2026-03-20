package cc.ivera.test.base;

import org.apache.tika.Tika;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;

/**
 * tika文件类型判断
 */
public class Demo9 {
    public static void main(String[] args) throws IOException {
        Tika tika = new Tika();
        String mimeType = tika.detect(new File("D:\\develop\\media\\test.png"));
        MediaType mediaType = MediaType.parseMediaType(mimeType);
        System.out.println(mimeType);
        System.out.println(mediaType);
    }
}
