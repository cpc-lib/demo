package cc.ivera.test.base;

import org.apache.commons.lang3.StringUtils;
import cc.ivera.util.FileUtil;

import java.io.File;

public class Demo79 {


    public static String getFileType(String fileName) {
        if (StringUtils.isNotEmpty(fileName)) {
            int i = fileName.lastIndexOf(".");
            return fileName.substring(i + 1);
        } else {
            return "";
        }
    }

    public static void main(String[] args) {
        String filePath = "D:\\develop\\code\\cs-spring\\leetcode\\src\\main\\resources\\demo2.jpg";
        String md5 = FileUtil.calculateMD5(filePath);
        String fileType = getFileType(filePath);
        System.out.println("MD5: " + md5);
        System.out.println(fileType);
        //获取文件的大小
        System.out.println(FileUtil.getPrintSize(new File(filePath).length()));
    }


}
