package cc.ivera.domain.pojo;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@TableName("tb_user")
public class User implements Serializable {

    @TableId(type = IdType.AUTO)
    @ExcelProperty(value = "主键ID", index = 0)
    private Integer id;

    @ExcelProperty(value = "等级", index = 1)
    private Integer userRank;

    @ExcelProperty(value = "名称", index = 2)
    private String name;

    @ExcelProperty(value = "创建时间", index = 3)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @ExcelProperty(value = "城市", index = 4)
    private String city;

    @ExcelProperty(value = "身份证号", index = 5)
    private String idCard;

    @ExcelProperty(value = "邮箱地址", index = 6)
    private String email;

}
