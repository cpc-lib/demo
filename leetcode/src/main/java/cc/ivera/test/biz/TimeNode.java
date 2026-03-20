package cc.ivera.test.biz;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TimeNode implements Serializable {
    private Date nodeStartTime;
    private Date nodeEndTime;
    private BigDecimal payment;
}
