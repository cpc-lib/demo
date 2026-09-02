package cc.ivera.entity;

import lombok.Data;

import java.util.Date;

@Data
public class MessageConsumeLog {

    private Long id;

    private String eventId;

    private String consumerName;

    private String eventType;

    private String businessKey;

    private String status;

    private String lockedBy;

    private Date lockExpireTime;

    private String lastError;

    private Date consumedAt;
}
