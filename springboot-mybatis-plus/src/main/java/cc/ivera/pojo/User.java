package cc.ivera.pojo;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    //本次设置测试驼峰配置为false


    //TableId声明为一个主键,对应数据表中的字段，查询使用keyId进行查询
    //set设置到id字段(lombok)
    //SELECT keyId AS id,pass_word,username,ip_address
    //@TableId(value="keyId")
    private Long id;


    //此字段password，作为sql拼接语句,查询进行映射
    //声明为设置select为false,不对此字段进行set操作。
    //@TableField(select=false)
    private String password;

    private String username;

    //设置此字段不存在与数据表中
    //@TableField(exist=false)

    //mybatisplus配置配置为true,查询字段转换为驼峰后进行凭借sql语句
    //设置为false需要@TableField(value="ip_address")注解帮助，查询无bug
    //@TableField(value="ip_address")
    //private String ipAddress;


    //进行驼峰映射设置为ture去掉此项
    //private String ip_address;

    //不设置mybatisplus的驼峰配置，设置mybatis的驼峰配置为false驼峰，mybatisconfiguration的驼峰值然为true
    private String ip_address;





    //mybatisplus的通用方法
    //mybatisplus 默认实体类的字段都在数据中存在，根据实体类字段排序进行查询数据
    //声明@TableField(exist=false)不凭借到sql语句中
    //如果实体类字段与数据库字段不一致设置@TableField(name="ip_address")，以该name字段作为sql拼接字段，然后set到实体类的该注解下的字段



}
