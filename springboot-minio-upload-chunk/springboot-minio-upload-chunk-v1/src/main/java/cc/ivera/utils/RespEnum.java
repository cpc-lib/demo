package cc.ivera.utils;

public enum RespEnum {

    UPLOADSUCCESSFUL(1, "上传成功"),
    UPLOADING(2, "上传中"),
    NOT_UPLOADED(3, "未上传"),
    ACCESS_PARAMETER_INVALID(1001,"访问参数无效"),
    UPLOAD_FILE_FAILED(1002,"文件上传失败"),
    DATA_NOT_EXISTS(1003,"数据不存在"),
    ;

    private final Integer code;
    private final String message;

    public Integer getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    RespEnum(Integer code, String message) {
        this.code = code;
        this.message = message;
    }
}
