package cc.ivera.model.pojo.easyexcel;

import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.DateTimeFormat;
import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class User2Excel {

    private String sheetName;

    private List<User2> itemList;

    @Data
    public static class User2 {

        @ExcelProperty(value = "主键ID", index = 0)
        private Integer id;

        @ExcelProperty(value = "等级", index = 1)
        private Integer rank;

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

}
