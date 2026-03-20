package cc.ivera.test.base;

import cc.ivera.util.ImageUtil;

import java.io.File;

/**
 * @Description
 * @Author 三文鱼先生
 * @Data 2023/7/5 14:38
 */
public class Demo72 {
    public static void main(String[] args) {

        String baseDir = "D:\\develop\\code\\cs-java\\leetcode\\pic";

        String inputImagePath = baseDir + File.separator + "origin.jpg";

        String thumbnailPath = baseDir + File.separator + "thumbnail.jpg";

        String compressPath = baseDir + File.separator + "compress.jpg";

        ImageUtil.compressWithJPEG(inputImagePath, compressPath);

        ImageUtil.storeThumbnailWithImage(inputImagePath, thumbnailPath);
    }
}
