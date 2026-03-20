package cc.ivera.domain;

import cc.ivera.enums.StateEnum;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("account")
public class Account extends BaseEntity {

    @TableId(value="account_id",type= IdType.INPUT)
    private String accountId;

    private String accountName;

    /**
     * mp的枚举
     */
    private StateEnum state;

}
