package cc.ivera.openpdf.service;

public interface PdfSecurityService {

    void decrypt(String inputPdfPath, String outputPdfPath, String userOrOwnerPassword);
}
