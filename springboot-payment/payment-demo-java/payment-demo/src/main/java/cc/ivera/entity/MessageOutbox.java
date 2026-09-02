package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_message_outbox")
public class MessageOutbox extends BaseEntity {

    private String eventId;

    private String eventKey;

    private String aggregateType;

    private String aggregateId;

    private String eventType;

    private String payload;

    private String status;

    private Integer retryCount;

    private Date nextRetryTime;

    private String lockedBy;

    private Date lockExpireTime;

    private String lastError;

    private Date sentAt;
}
