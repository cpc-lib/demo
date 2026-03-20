package cc.ivera.model;

public class FileResource {
    /**
     * 文件名
     */
    private String fileName;
    /**
     * 文件后缀
     */
    private String suffixName;
    /**
     * 文件大小
     */
    private Long size;

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getSuffixName() {
        return suffixName;
    }

    public void setSuffixName(String suffixName) {
        this.suffixName = suffixName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }
}
