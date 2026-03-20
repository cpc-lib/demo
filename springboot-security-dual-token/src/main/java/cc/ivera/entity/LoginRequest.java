package cc.ivera.entity;


import com.sun.istack.internal.NotNull;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
/**
 * 登录请求对象
 * @author LiYunFei
 * @date 2023/6/20 21:56
 */
@ApiModel(value ="LoginRequest",description = "登录实体")
@AllArgsConstructor
@Data
public class LoginRequest {
    @NotNull
    @ApiModelProperty("用户账号")
    private String username;
    @NotNull
    @ApiModelProperty("用户密码")
    private String password;
    /**
     * 是否记住密码，pc可勾选，app默认记住
     */
    @ApiModelProperty("是否记住")
    private Boolean rememberMe;
}
