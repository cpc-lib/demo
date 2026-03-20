package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.io.Serializable;

@TableName("user")
@Data
public class User implements Serializable {

    //@TableField("id")
    private Integer id;
    //@TableField("username")
    private String username;
    //@TableField("password")
    private String password;

}
