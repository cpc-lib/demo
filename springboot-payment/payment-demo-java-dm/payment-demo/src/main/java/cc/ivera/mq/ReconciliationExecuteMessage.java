package cc.ivera.mq;

import lombok.Data;

import java.io.Serializable;

@Data
public class ReconciliationExecuteMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private String batchNo;

    private Integer retryCount;
}
