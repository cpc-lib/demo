package cc.ivera.domain.bo;

import java.util.Date;
import java.util.Objects;

public class BillCycleBO {
    private Date startTime;
    private Date endTime;
    /**
     * 1-固定账期整段，0-普通账期拆分
     */
    private String wholeFlag;

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

    public String getWholeFlag() {
        return wholeFlag;
    }

    public void setWholeFlag(String wholeFlag) {
        this.wholeFlag = wholeFlag;
    }

    @Override
    public String toString() {
        return "BillCycleBO{" +
                "startTime=" + startTime +
                ", endTime=" + endTime +
                ", wholeFlag='" + wholeFlag + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BillCycleBO billCycleBO)) {
            return false;
        }
        return Objects.equals(startTime, billCycleBO.startTime)
                && Objects.equals(endTime, billCycleBO.endTime)
                && Objects.equals(wholeFlag, billCycleBO.wholeFlag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startTime, endTime, wholeFlag);
    }
}
