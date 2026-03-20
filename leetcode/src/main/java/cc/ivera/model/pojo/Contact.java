//package cc.ivera.model.pojo;
//
//import com.baomidou.mybatisplus.annotation.IdType;
//import com.baomidou.mybatisplus.annotation.TableField;
//import com.baomidou.mybatisplus.annotation.TableId;
//import com.baomidou.mybatisplus.annotation.TableName;
//import lombok.Data;
//import lombok.NoArgsConstructor;
//import cc.ivera.mp.handler.EncryptHandler;
//
//import java.io.Serializable;
//
//
//@Data
//@NoArgsConstructor
//@TableName(value = "contact", autoResultMap = true)
//public class Contact implements Serializable {
//    @TableId(type = IdType.AUTO)
//    private Integer id;
//
//    private String name;
//
//    @TableField(typeHandler = EncryptHandler.class)
//    private String phone;
//
//    private String gender;
//}