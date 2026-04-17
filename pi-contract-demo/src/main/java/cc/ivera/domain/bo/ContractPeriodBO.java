package cc.ivera.domain.bo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

public class ContractPeriodBO implements Serializable {
    private String contractManageId;
    private String contractPeriodId;
    private Date startTime;
    private Date endTime;
    private BigDecimal payment;
    /**
     * 是否整段使用固定账期：1-是，0-否
     */
    private String wholeFlag;

    public ContractPeriodBO() {
    }

    public ContractPeriodBO(String contractManageId, String contractPeriodId, Date startTime, Date endTime,
                            BigDecimal payment, String wholeFlag) {
        this.contractManageId = contractManageId;
        this.contractPeriodId = contractPeriodId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.payment = payment;
        this.wholeFlag = wholeFlag;
    }

    public String getContractManageId() {
        return contractManageId;
    }

    public void setContractManageId(String contractManageId) {
        this.contractManageId = contractManageId;
    }

    public String getContractPeriodId() {
        return contractPeriodId;
    }

    public void setContractPeriodId(String contractPeriodId) {
        this.contractPeriodId = contractPeriodId;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(BigDecimal payment) {
        this.payment = payment;
    }

    public String getWholeFlag() {
        return wholeFlag;
    }

    public void setWholeFlag(String wholeFlag) {
        this.wholeFlag = wholeFlag;
    }

    @Override
    public String toString() {
        return "ContractPeriodBO{" +
                "contractManageId='" + contractManageId + '\'' +
                ", contractPeriodId='" + contractPeriodId + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", payment=" + payment +
                ", wholeFlag='" + wholeFlag + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContractPeriodBO that)) {
            return false;
        }
        return Objects.equals(contractManageId, that.contractManageId)
                && Objects.equals(contractPeriodId, that.contractPeriodId)
                && Objects.equals(startTime, that.startTime)
                && Objects.equals(endTime, that.endTime)
                && Objects.equals(payment, that.payment)
                && Objects.equals(wholeFlag, that.wholeFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(contractManageId, contractPeriodId, startTime, endTime, payment, wholeFlag);
    }
}
