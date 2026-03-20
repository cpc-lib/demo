package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import nonapi.io.github.classgraph.json.Id;

import java.io.Serializable;

/**
 *用户实体类
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("user_db")
public class User implements Serializable {
    @TableId
    private Integer id;

    private String username;
    
}
