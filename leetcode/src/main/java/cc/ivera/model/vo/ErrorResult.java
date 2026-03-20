package cc.ivera.model.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorResult {
    private String code;
    private String message;

    public static ErrorResult error() {
        return ErrorResult.builder().code("999999").message("系统异常稍后再试").build();
    }

    public static ErrorResult fail() {
        return ErrorResult.builder().code("000001").message("发送验证码失败").build();
    }

    public static ErrorResult loginError() {
        return ErrorResult.builder().code("000002").message("验证码失效").build();
    }

    public static ErrorResult faceError() {
        return ErrorResult.builder().code("000003").message("图片非人像，请重新上传!").build();
    }

    public static ErrorResult mobileError() {
        return ErrorResult.builder().code("000004").message("手机号码已注册").build();
    }


    public static ErrorResult contentError() {
        return ErrorResult.builder().code("000005").message("动态内容为空").build();
    }

    public static ErrorResult likeError() {
        return ErrorResult.builder().code("000006").message("已点赞").build();
    }

    public static ErrorResult dislikeError() {
        return ErrorResult.builder().code("000007").message("已取消点赞").build();
    }
}
