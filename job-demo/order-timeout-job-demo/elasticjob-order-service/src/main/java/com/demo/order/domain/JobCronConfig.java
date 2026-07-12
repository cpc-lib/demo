package com.demo.order.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("job_cron_config")
public class JobCronConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobType;
    private String jobName;
    private String cronExpr;
    private Integer enabled;
    private LocalDateTime updatedAt;
}
