package cc.ivera.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractPeriodVo implements Serializable {
    private Integer number;
    private Date periodStart;
    private Date periodEnd;
    private Integer year;
    private BigDecimal monthRent;
    private String normalFlag;
    private String yearMonth;
}
