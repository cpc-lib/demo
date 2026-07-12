package com.demo.order.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("order_close_job_log")
public class OrderCloseJobLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String jobType;
    private String instanceId;
    private String batchNo;
    private Integer scannedCount;
    private Integer closedCount;
    private String status;
    private String errorMsg;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
