package cc.ivera.migration.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ApiResult<T> {
    private boolean success;
    private String message;
    private T data;
    public static <T> ApiResult<T> ok(T data) { return new ApiResult<>(true, "OK", data); }
    public static <T> ApiResult<T> fail(String msg) { return new ApiResult<>(false, msg, null); }
}
