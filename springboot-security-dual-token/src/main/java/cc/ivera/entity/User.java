package cc.ivera.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户实体
 * @Author: LiYunFei
 * @Date: 2022/6/20 15:48
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class User {
    private Long id;
    private String username;
    private String password;

    private Boolean enabled;
    /**
     * 是否记住密码，pc可勾选，app默认记住
     */
    private Boolean rememberMe;

}
