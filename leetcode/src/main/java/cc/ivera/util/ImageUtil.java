package cc.ivera.util;


import cn.hutool.core.lang.Assert;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * TODO 如何与web接入 需要文件表记录操作,更好的操作我觉得是保存在minio文件服务 md5计算妙传 图片过大可以引入断电续传
 * https://blog.csdn.net/qq_44717657/article/details/131554741
 *
 * @Description 用于有损压缩图片以及获取缩略图
 * @Author 三文鱼先生
 * @Data 2023/7/5 10:02
 */
public class ImageUtil {

    /**
     * @Description 以JPEG格式压缩图片
     * @Param inputImage 输入的图片
     * @Param outputImage 输出的图片
     * @Return boolean 压缩成功则返回true 否则返回false
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:04
     **/
    public static boolean compressWithJPEG(String inputPath, String outputPath) {

        File inputImage = new File(inputPath);

        File outputImage = new File(outputPath);

        //图像的输出流
        ImageOutputStream outputStream = null;

        try {
            //防止红色调 不再使用ImageIO.read()读取
            //getImage方法读取方式为异步读取 所以需要媒体跟踪器确保图片加载完成
            Image imageTemp = Toolkit.getDefaultToolkit().
                    getImage(inputImage.getAbsolutePath());
//            Image imageTemp =ImageIO.read(inputImage);
            //媒体跟踪对象 用来跟踪图片的加载状态
            MediaTracker mediaTracker = new MediaTracker(new Component() {
            });
            //跟踪当前图片
            mediaTracker.addImage(imageTemp, 0);
            //等待图片加载完成才能执行后续操作
            mediaTracker.waitForAll();

            //将Image对象转为BufferedImage对象便于后续操作
            BufferedImage image = toBufferedImage(imageTemp);

            //获取JPEG格式的ImageWriter 来实现压缩 用于写出图片
            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();

            //创建ImageWriteParam 并设置压缩参数
            ImageWriteParam param = writer.getDefaultWriteParam();
            //设置压缩模式为有损压缩
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            //以最高质量压缩 0.0 - 1.0之间
            param.setCompressionQuality(1.0F);
            //使用图像文件中的元数据来确定是否使用平铺模式
            param.setProgressiveMode(ImageWriteParam.MODE_COPY_FROM_METADATA);

            //获取一个图像输出流
            outputStream = ImageIO.createImageOutputStream(outputImage);
            //设置写出的输出流
            writer.setOutput(outputStream);
            //写出
            writer.write(
                    //图片额外数据为空
                    null,
                    new IIOImage(
                            //基本的image图像
                            image,
                            //缩略图为空
                            null,
                            //图片额外信息为空
                            null
                    ),
                    //压缩参数
                    param
            );
            //释放资源
            writer.dispose();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * @Description 将image图像转为BufferedImage操作
     * @Param image 需要转换的图像
     * @Return {@link BufferedImage}
     * @Author 三文鱼先生
     * @Date 2023/7/4 9:46
     **/
    public static BufferedImage toBufferedImage(Image image) {
        BufferedImage bufferedImage = new BufferedImage(
                //传入图像的宽 不设置观察者
                image.getWidth(null),
                //传入图像的高 不设置观察者
                image.getHeight(null),
                //RGB颜色
                BufferedImage.TYPE_INT_RGB
        );
        //获取用于绘图的画布对象
        Graphics2D graphics2D = bufferedImage.createGraphics();
        //相当与重新绘制原原图形一次
        graphics2D.drawImage(image,
                //x轴的开始坐标
                0,
                //y轴的开始坐标
                0,
                //观察者
                null);
        //释放资源
        graphics2D.dispose();
        return bufferedImage;
    }


    /**
     * @Description 不设置宽高 按照同比例缩放 并且存储缩略图 生成的图片格式默认为jpeg
     * @Param inFilePath 需要压缩的图片    路径
     * @Param storePath 生成缩略图存储的图片路径
     * @Return {@link boolean}
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:43
     **/
    public static boolean storeThumbnailWithImage(
            String inFilePath,
            String storePath
    ) {
        //默认为宽为500的同比例缩放 格式为jpeg
        return storeImage(createThumbnail(inFilePath, 0, 0),
                storePath, "jpeg");
    }

    /**
     * @Description 以指定宽高生成缩略图
     * @Param inFilePath 同上
     * @Param storePath 同上
     * @Param width 缩略图宽度
     * @Param height 缩略图高度
     * @Return {@link boolean}
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:46
     **/
    public static boolean storeThumbnailWithImage(
            String inFilePath,
            String storePath,
            int width,
            int height
    ) {
        return storeImage(createThumbnail(inFilePath, width, height),
                storePath, "jpeg");
    }

    /**
     * @Description 同上
     * @Param inFilePath 同上
     * @Param storePath 同上
     * @Param width 同上
     * @Param height 同上
     * @Param type 以什么格式生成缩略图 一般是jpg或者jpeg  png格式的大小是前者的十倍
     * @Return {@link boolean}
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:47
     **/
    public static boolean storeThumbnailWithImage(
            String inFilePath,
            String storePath,
            int width,
            int height,
            String type
    ) {
        return storeImage(createThumbnail(inFilePath, width, height),
                storePath, type);
    }

    /**
     * @Description 获取一个缩略图的BufferedImage 宽高决定了图像文件的大小
     * @Param filePath 原图像路径
     * @Param width 缩略图宽度
     * @Param height 缩略图高度
     * @Return {@link BufferedImage}
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:49
     **/
    public static BufferedImage createThumbnail(
            String filePath,
            int width,
            int height
    ) {
        try {
            //获取一个对应路径的图像
            File imageFile = new File(filePath);
            //false才触发
            Assert.isTrue(imageFile.exists(), "文件不存在！");
            BufferedImage originalImage = ImageIO.read(imageFile);

            if (width == 0 && height == 0) {
                int defaultWidth = 0;
                width = originalImage.getWidth();
                height = originalImage.getHeight();
                if (width > 2000) {
                    defaultWidth = width / 5;
                } else if (width >= 1000) {
                    defaultWidth = width / 3;
                } else if (width >= 500) {
                    defaultWidth = width / 2;
                } else {
                    defaultWidth = 100;
                }
                //同比例缩放
                height = (int) (((double) height / (double) width) * defaultWidth);
                width = defaultWidth;
            }

            // 创建一个缩略图 BufferedImage 对象
            BufferedImage thumbnailImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

            // 使用 Graphics2D 进行绘制操作
            Graphics2D graphics2D = thumbnailImage.createGraphics();
            //设置渲染提示
            graphics2D.setRenderingHint(
                    //用于在缩放或变换图像时进行像素之间的插值 它影响图像的平滑度和细节保留程度
                    RenderingHints.KEY_INTERPOLATION,
                    //该值表示使用双线性插值算法进行图像的插值。双线性插值是一种平滑的插值方法
                    //会在缩放时通过对周围像素的加权平均来计算新像素的值，以产生更平滑的图像。
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR
            );
            //绘制缩略图
            graphics2D.drawImage(
                    //原始图像
                    originalImage,
                    //绘制x轴的起点
                    0,
                    //y轴起点
                    0,
                    //x轴终点
                    width,
                    //y轴终点
                    height,
                    //观察者设置为空
                    null
            );
            //释放画布资源
            graphics2D.dispose();
            return thumbnailImage;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @Description 以指定格式写出图像存储到指定路径 文件后缀名最好与存储格式一致
     * @Param storeImage 需要存储的图像
     * @Param storePath 存储路径
     * @Param type 存储格式
     * @Return {@link boolean}
     * @Author 三文鱼先生
     * @Date 2023/7/5 10:50
     **/
    public static boolean storeImage(BufferedImage storeImage, String storePath, String type) {
        try {
            Assert.notNull(storeImage, "图像不能为空！");
            File storeFile = new File(storePath);
            if (storeFile.exists()) {
                Assert.isTrue(storeFile.delete(), "当前文件: " + storePath + " 已存在，且未能删除。");
            }
            //写出图片到文件 格式为type
            ImageIO.write(storeImage, type, storeFile);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

