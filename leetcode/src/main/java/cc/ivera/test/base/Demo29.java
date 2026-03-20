//package cc.ivera.test.base;
//
//import org.apache.poi.xwpf.usermodel.XWPFDocument;
//import cc.ivera.util.Img2PdfUtil;
//import cc.ivera.util.Pdf2ImgUtil;
//
//import java.io.*;
//import java.nio.file.NoSuchFileException;
//import java.util.concurrent.ExecutionException;
//
//public class Demo29 {
//
//    public static void main(String[] args) throws IOException, InterruptedException, ExecutionException {
////        convertPdf2Img("E:\\BaiduNetdiskDownload\\Activiti实战.pdf",
////                "E:\\BaiduNetdiskDownload\\output\\",
////                "png",
////                100);
//        convertImg2pdf("E:\\BaiduNetdiskDownload\\output", "Activiti实战.pdf");
//    }
//
//    private static void convertWord2PdfByPOI() {
//        try {
//            InputStream docFile = new FileInputStream(new File("E:\\develop\\gitee.com\\repo\\funplus\\public\\DocToPdf\\src\\main\\resources\\funplus.docx"));
//            XWPFDocument doc = new XWPFDocument(docFile);
//            PdfOptions pdfOptions = PdfOptions.create();
//            OutputStream out = new FileOutputStream(new File("E:\\develop\\gitee.com\\repo\\funplus\\public\\DocToPdf\\src\\main\\resources\\funplus.pdf"));
//            PdfConverter.getInstance().convert(doc, out, pdfOptions);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    private static void convertPdf2Img(String source, String targetDir, String imgType, int dpi) throws IOException {
//        File pdfFile = new File(source);
//        if (!pdfFile.exists()) {
//            throw new NoSuchFileException(pdfFile.getAbsolutePath());
//        }
//        File dir = new File(targetDir);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//        Pdf2ImgUtil.pdf2Img(source);
//    }
//
//    private static void convertImg2pdf(String imagesPath, String destPath) {
//        Img2PdfUtil.imagesToPdf(imagesPath, destPath);
//    }
//}
