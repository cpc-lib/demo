package cc.ivera.common.bean;

import lombok.Data;

/**
 * @author Administrator
 * @version 1.0
 * @description: TODO
 * @date 2021/9/15 20:11
 */
@Data
public class User {
    private String userName;
    private Integer age;
    private Address address;
}
