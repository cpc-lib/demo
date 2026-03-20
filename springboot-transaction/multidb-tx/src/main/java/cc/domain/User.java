package cc.ivera.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * @author: 犬小哈
 * @date: 2022-12-13 14:13
 * @version: v1.0.0
 * @description: TODO
 **/
@Data
@TableName("t_user")
public class User {
    /**
     * 主键 ID, @TableId 注解定义字段为表的主键，type 表示主键类型，IdType.AUTO 表示随着数据库 ID 自增
     */
    @TableId(type = IdType.AUTO)
    private Long id;
    /**
     * 姓名
     */
    private String name;
    /**
     * 年龄
     */
    private Integer age;
    /**
     * 性别
     */
    private Integer gender;
}