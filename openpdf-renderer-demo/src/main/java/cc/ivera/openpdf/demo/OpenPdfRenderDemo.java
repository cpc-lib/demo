package cc.ivera.openpdf.demo;

import cc.ivera.openpdf.model.request.PdfRenderRequest;
import cc.ivera.openpdf.service.PdfRenderService;
import cc.ivera.openpdf.service.PdfSecurityService;
import cc.ivera.openpdf.service.impl.OpenPdfRenderService;
import cc.ivera.openpdf.service.impl.OpenPdfSecurityService;

import java.nio.file.Path;

/**
 * 运行示例。
 */
public class OpenPdfRenderDemo {

    public static void main(String[] args) {
        String projectDir = System.getProperty("user.dir");
        String outputDir = projectDir + "/target/generated-pdf";
        String imagePath = projectDir + "/src/main/resources/images/sample-logo.png";

        PdfRenderService renderService = new OpenPdfRenderService();
        PdfSecurityService securityService = new OpenPdfSecurityService();

        PdfRenderRequest request = SampleRequestFactory.create(
                outputDir + "/business-report-encrypted.pdf",
                imagePath
        );

        renderService.render(request);
        securityService.decrypt(
                outputDir + "/business-report-encrypted.pdf",
                outputDir + "/business-report-decrypted.pdf",
                "123456"
        );

        System.out.println("Encrypted PDF: " + Path.of(outputDir, "business-report-encrypted.pdf"));
        System.out.println("Decrypted PDF: " + Path.of(outputDir, "business-report-decrypted.pdf"));
    }
}
