package cc.ivera.util;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Pdf2ImgUtil {
    /**
     * pdf转图片
     * 转换全部的pdf
     *
     * @param fileAddress PDF文件绝对路径
     * @return ArrayList<String>    生成imgs文件群的绝对路径集合
     */
    public static List<String> pdf2Img(String fileAddress) throws IOException {
        // 将pdf装图片 并且自定义图片得格式大小
        File file = new File(fileAddress);
        ArrayList<String> fileAddresss = new ArrayList<>();
        PDDocument doc = null;
        try {
            doc = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            for (int i = 0; i < pageCount; i++) {
//经过测试,dpi为96,100,105,120,150,200中,105显示效果较为清晰,体积稳定,dpi越高图片体积越大,一般电脑显示分辨率为96
                BufferedImage image = renderer.renderImageWithDPI(i, 105); // Windows native DPI
                // BufferedImage srcImage = resize(image, 240, 240);//产生缩略图
                String substring = fileAddress.substring(0, fileAddress.lastIndexOf("."));
                ImageIO.write(image, "jpg", new File(substring + "_" + i + ".jpg"));
                fileAddresss.add(substring + "_" + i + ".jpg");
            }
        } catch (IOException e) {
            throw new IOException("pdf转img工具类异常");
        } finally {
            if (doc != null) {
                doc.close();
            }
        }
        return fileAddresss;
    }


    /**
     * 自由确定起始页和终止页
     *
     * @param fileAddress  文件绝对路径
     * @param indexOfStart 开始页  开始转换的页码，从0开始
     * @param indexOfEnd   结束页  停止转换的页码，
     */
    public static void pdf3Img(String fileAddress, int indexOfStart, int indexOfEnd) throws IOException {
        // 将pdf装图片 并且自定义图片得格式大小
        File file = new File(fileAddress);
        PDDocument doc = null;
        try {
            doc = PDDocument.load(file);
            PDFRenderer renderer = new PDFRenderer(doc);
            int pageCount = doc.getNumberOfPages();
            for (int i = indexOfStart; i < indexOfEnd; i++) {
                BufferedImage image = renderer.renderImageWithDPI(i, 105); // Windows native DPI
                // BufferedImage srcImage = resize(image, 240, 240);//产生缩略图
                String substring = fileAddress.substring(0, fileAddress.lastIndexOf("."));
                ImageIO.write(image, "jpg", new File(substring + "_" + (i + 1) + ".jpg"));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (doc != null) {
                doc.close();
            }
        }
    }

    private static BufferedImage resize(BufferedImage source, int targetW, int targetH) {
        int type = source.getType();
        BufferedImage target = null;
        double sx = (double) targetW / source.getWidth();
        double sy = (double) targetH / source.getHeight();
        if (sx > sy) {
            sx = sy;
            targetW = (int) (sx * source.getWidth());
        } else {
            sy = sx;
            targetH = (int) (sy * source.getHeight());
        }
        if (type == BufferedImage.TYPE_CUSTOM) {
            ColorModel cm = source.getColorModel();
            WritableRaster raster = cm.createCompatibleWritableRaster(targetW, targetH);
            boolean alphaPremultiplied = cm.isAlphaPremultiplied();
            target = new BufferedImage(cm, raster, alphaPremultiplied, null);
        } else {
            target = new BufferedImage(targetW, targetH, type);
        }
        Graphics2D g = target.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawRenderedImage(source, AffineTransform.getScaleInstance(sx, sy));
        g.dispose();
        return target;
    }
}
