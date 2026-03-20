package cc.ivera.model.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractPeriodBo implements Serializable {
    private String contractManageId;//合同id
    private String contractPeriodId;//阶段id
    private Date startTime;//阶段收费开始时间
    private Date endTime;//阶段收费结束时间
    private BigDecimal payment;//月度租金
    private String wholeFlag;//是否拆分
}
