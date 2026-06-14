package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("change_log_detail")
public class ChangeLogDetailRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long logId;

    private String fieldName;

    private String fieldLabel;

    private String oldValue;

    private String newValue;

    private String valueType;

    private Integer changed;

    private LocalDateTime createdAt;
}
