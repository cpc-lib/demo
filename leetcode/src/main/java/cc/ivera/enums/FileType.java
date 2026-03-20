package cc.ivera.enums;

public enum FileType {

    PNG("png", "img"),
    JPG("jpg", "img"),
    BMP("bmp", "img"),

    AVI("avi", "video"),
    MP4("mp4", "video"),
    MKV("mkv", "video"),
    FLV("flv", "video");


    private String extension;
    private String type;

    FileType(String extension, String type) {
        this.extension = extension;
        this.type = type;
    }

    public static FileType findByExtension(String extension) {
        if (extension != null && extension.length() > 0 && (!extension.contains(" "))) {
            for (FileType fileType : FileType.values()) {
                if (extension.equals(fileType.getExtension())) {
                    return fileType;
                }
            }
            return null;
        }
        return null;
    }

    public String getType() {
        return this.type;
    }

    public String getExtension() {
        return this.extension;
    }

}
