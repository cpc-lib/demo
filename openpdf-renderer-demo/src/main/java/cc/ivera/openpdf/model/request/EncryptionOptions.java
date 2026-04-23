package cc.ivera.openpdf.model.request;

import org.openpdf.text.pdf.PdfWriter;

/**
 * PDF 加密配置。
 */
public class EncryptionOptions {

    private String userPassword;
    private String ownerPassword;
    private int permissions = PdfWriter.ALLOW_PRINTING;
    private int encryptionType = PdfWriter.ENCRYPTION_AES_128;

    public String getUserPassword() {
        return userPassword;
    }

    public EncryptionOptions setUserPassword(String userPassword) {
        this.userPassword = userPassword;
        return this;
    }

    public String getOwnerPassword() {
        return ownerPassword;
    }

    public EncryptionOptions setOwnerPassword(String ownerPassword) {
        this.ownerPassword = ownerPassword;
        return this;
    }

    public int getPermissions() {
        return permissions;
    }

    public EncryptionOptions setPermissions(int permissions) {
        this.permissions = permissions;
        return this;
    }

    public int getEncryptionType() {
        return encryptionType;
    }

    public EncryptionOptions setEncryptionType(int encryptionType) {
        this.encryptionType = encryptionType;
        return this;
    }
}
