package cc.ivera.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 结果统一封装
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Result implements Serializable {

    private boolean success;

    private int code;

    private String msg;

    private Object data;


    /**
     * success方法，标识成功
     *
     * @param data
     * @return
     */
    public static Result success(Object data) {
        return new Result(true, 200, "success", data);
    }

    /**
     * fail方法，标识失败
     *
     * @param code
     * @param msg
     * @return
     */
    public static Result fail(int code, String msg) {
        return new Result(false, code, msg, null);
    }
}
