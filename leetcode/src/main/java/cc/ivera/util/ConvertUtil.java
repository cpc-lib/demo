package cc.ivera.util;

import com.aspose.words.License;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import cc.ivera.exception.BusinessException;
import cc.ivera.model.vo.ErrorResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;

@Slf4j
public class ConvertUtil {

    /**
     * 使用pdfbox将整个pdf转换成图片
     *
     * @param targetAddress 文件地址
     * @param filename      PDF文件名不带后缀名
     * @param type          图片类型 png 和jpg
     */
    public static void pdf2png(String targetAddress, String filename, String type, int dpi) {
        if (StringUtils.isNotEmpty(filename)) {
            String suffix = filename.substring(filename.lastIndexOf(".") + 1);
            if ("pdf".equals(suffix)) {
                File file = new File(filename);
                if (file.exists()) {
                    try {
                        String name = file.getName();
                        String fileName = name.substring(0, name.lastIndexOf("."));
                        // 写入文件
                        PDDocument doc = PDDocument.load(file);
                        PDFRenderer renderer = new PDFRenderer(doc);
                        int pageCount = doc.getNumberOfPages();
                        for (int i = 0; i < pageCount; i++) {
                            // dpi为144，越高越清晰，转换越慢
                            BufferedImage image = renderer.renderImageWithDPI(i, dpi); // Windows native DPI
                            // 将图片写出到该路径下
                            ImageIO.write(image, type, new File(targetAddress + File.separator + fileName + "_" + (i + 1) + "." + type));
                        }
                    } catch (IOException e) {
                        log.error("convert failed {}", e.getMessage());
                        throw new BusinessException(ErrorResult.error());
                    }
                } else {
                    throw new BusinessException(ErrorResult.error());
                }
            } else {
                throw new BusinessException(ErrorResult.error());
            }
        }
    }

    /**
     * @param targetAddress 存放位置
     * @param filename      输入文件路径
     * @param indexOfStart  开始页码
     * @param indexOfEnd    结束页码
     * @param type          图片类型
     * @param dpi
     */
    public static void pdf2png(String targetAddress, String filename, int indexOfStart, int indexOfEnd, String type, int dpi) {
        if (StringUtils.isNotEmpty(filename)) {
            String suffix = filename.substring(filename.lastIndexOf(".") + 1);
            if ("pdf".equals(suffix)) {
                File file = new File(filename);
                if (file.exists()) {
                    try {
                        String name = file.getName();
                        String fileName = name.substring(0, name.lastIndexOf("."));
                        // 写入文件
                        PDDocument doc = PDDocument.load(file);
                        PDFRenderer renderer = new PDFRenderer(doc);
                        int pageCount = doc.getNumberOfPages();
                        for (int i = indexOfStart; i < indexOfEnd; i++) {
                            // dpi为144，越高越清晰，转换越慢
                            BufferedImage image = renderer.renderImageWithDPI(i, dpi); // Windows native DPI
                            // 将图片写出到该路径下
                            ImageIO.write(image, type, new File(targetAddress + File.separator + fileName + "_" + (i + 1) + "." + type));
                        }
                    } catch (IOException e) {
                        log.error("convert failed {}", e.getMessage());
                        throw new BusinessException(ErrorResult.error());
                    }
                } else {
                    throw new BusinessException(ErrorResult.error());
                }
            } else {
                throw new BusinessException(ErrorResult.error());
            }
        }
    }


    /**
     * itext
     *
     * @param imageUrllist
     * @param mOutputPdfFileName
     * @return
     */
    public static File pdf(ArrayList<String> imageUrllist, String mOutputPdfFileName) {
        Document doc = new Document(PageSize.A4, 0, 0, 0, 0); //new一个pdf文档
        try {
            PdfWriter.getInstance(doc, new FileOutputStream(mOutputPdfFileName)); //pdf写入
            doc.open();//打开文档
            for (int i = 0; i < imageUrllist.size(); i++) {  //循环图片List，将图片加入到pdf中
                doc.newPage();  //在pdf创建一页
                Image png1 = Image.getInstance(imageUrllist.get(i)); //通过文件路径获取image
                float heigth = png1.getHeight();
                float width = png1.getWidth();
                int percent = getPercent2(heigth, width);
                png1.setAlignment(Image.MIDDLE);
                png1.scalePercent(percent + 3);// 表示是原来图像的比例;
                doc.add(png1);
            }
            doc.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (DocumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        File mOutputPdfFile = new File(mOutputPdfFileName);  //输出流
        if (!mOutputPdfFile.exists()) {
            mOutputPdfFile.deleteOnExit();
            return null;
        }
        return mOutputPdfFile; //反回文件输出流
    }

    public static int getPercent(float h, float w) {
        int p = 0;
        float p2 = 0.0f;
        if (h > w) {
            p2 = 297 / h * 100;
        } else {
            p2 = 210 / w * 100;
        }
        p = Math.round(p2);
        return p;
    }

    public static int getPercent2(float h, float w) {
        int p = 0;
        float p2 = 0.0f;
        p2 = 480 / w * 100;
        p = Math.round(p2);
        return p;
    }

    /**
     * @Description: 通过图片路径及生成pdf路径，将图片转成pdf
     * @Author: zd
     * @Date: 2019/9/29
     */
    public static void imgOfPdf(String filepath, String imgUrl) {
        try {
            ArrayList<String> imageUrllist = new ArrayList<String>(); //图片list集合
            String[] imgUrls = imgUrl.split(",");
            for (int i = 0; i < imgUrls.length; i++) {
                imageUrllist.add(imgUrls[i]);
            }
            String pdfUrl = filepath;  //输出pdf文件路径
            File file = pdf(imageUrllist, pdfUrl);//生成pdf
            file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void imagesToPdf(String imagesPath, String destPath) {
        try {
            // 创建一个document对象。
            Document document = new Document();
            document.setMargins(0, 0, 0, 0);
            // 创建一个PdfWriter实例，
            PdfWriter.getInstance(document, new FileOutputStream(destPath));
            // 打开文档
            document.open();
            // 在文档中增加图片。
            File files = new File(imagesPath);
            String[] images = files.list();
            int len = images.length;

            for (int i = 0; i < len; i++) {
                if (images[i].toLowerCase().endsWith(".png")) {
                    String temp = imagesPath + "\\" + images[i];
                    Image img = Image.getInstance(temp);
                    img.setAlignment(Image.ALIGN_CENTER);
                    // 根据图片大小设置页面，一定要先设置页面，再newPage（），否则无效
                    document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
                    document.newPage();
                    document.add(img);
                }
            }
            // 关闭文档。
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * @param wordPath word路径
     * @param pdfPath  pdf输出路径
     * @Description word转pdf
     * @Date 14:31 2024/01/25
     * @author weixinxin
     */
    public static void word2Pdf(String wordPath, String pdfPath) throws Exception {
        if (!getLicense()) {
            return;
        }
        FileOutputStream os = null;
        try {
            long old = System.currentTimeMillis();
            File file = new File(pdfPath);
            os = new FileOutputStream(file);
            com.aspose.words.Document doc = new com.aspose.words.Document(wordPath);
            doc.save(os, com.aspose.words.SaveFormat.PDF);
            long now = System.currentTimeMillis();
            System.out.println("共耗时：" + ((now - old) / 1000.0) + "秒"); // 转化用时
        } finally {
            if (os != null) {
                os.close();
            }
        }
    }

    private static boolean getLicense() throws Exception {
        boolean result = false;
        ByteArrayInputStream is = null;
        try {
            String licenseXml = "<License><Data><Products><Product>Aspose.Total for Java</Product><Product>Aspose.Words for Java</Product></Products><EditionType>Enterprise</EditionType><SubscriptionExpiry>？？？？</SubscriptionExpiry><LicenseExpiry>？？？？</LicenseExpiry><SerialNumber>？？？？？</SerialNumber></Data><Signature>？？？？</Signature></License>";
            is = new ByteArrayInputStream(licenseXml.getBytes());
            License license = new License();
            license.setLicense(is);
            is.close();
            result = true;
        } finally {
            if (is != null) {
                is.close();
            }
        }
        return result;
    }


}

