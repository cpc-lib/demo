package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("change_log")
public class ChangeLogRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String bizType;

    private String bizId;

    private String operationType;

    private String logMode;

    private String operator;

    private String methodName;

    private String requestUri;

    private Integer changedFieldCount;

    private String remark;

    private LocalDateTime createdAt;
}
