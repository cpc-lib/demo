package cc.ivera.domain.bo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * 每个段收费的金额
 */
public class TimeNodeBO implements Serializable {
    private Date nodeStartTime;
    private Date nodeEndTime;
    private BigDecimal payment;

    public Date getNodeStartTime() {
        return nodeStartTime;
    }

    public void setNodeStartTime(Date nodeStartTime) {
        this.nodeStartTime = nodeStartTime;
    }

    public Date getNodeEndTime() {
        return nodeEndTime;
    }

    public void setNodeEndTime(Date nodeEndTime) {
        this.nodeEndTime = nodeEndTime;
    }

    public BigDecimal getPayment() {
        return payment;
    }

    public void setPayment(BigDecimal payment) {
        this.payment = payment;
    }

    @Override
    public String toString() {
        return "TimeNodeBO{" +
                "nodeStartTime=" + nodeStartTime +
                ", nodeEndTime=" + nodeEndTime +
                ", payment=" + payment +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TimeNodeBO timeNodeBO)) {
            return false;
        }
        return Objects.equals(nodeStartTime, timeNodeBO.nodeStartTime)
                && Objects.equals(nodeEndTime, timeNodeBO.nodeEndTime)
                && Objects.equals(payment, timeNodeBO.payment);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeStartTime, nodeEndTime, payment);
    }
}
