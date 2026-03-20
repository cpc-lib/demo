package cc.ivera.domain;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class User implements Serializable {

    @ExcelProperty(value="主键ID",index=0)
    private Integer id;

    @ExcelProperty(value="名称",index=2)
    private String name;

    @ExcelProperty(value="等级",index=1)
    private Integer rank;

    @ExcelProperty(value="创建时间",index=3)
    private String createtime;

    @ExcelProperty(value="城市",index=4)
    private String city;

    @ExcelProperty(value="邮箱地址",index=6)
    private String email;

    @ExcelProperty(value="身份证号",index=5)
    private String idcard;

}
