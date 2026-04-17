package cc.ivera.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

public class ContractPeriodVO implements Serializable {
    private Integer number;
    private Date periodStart;
    private Date periodEnd;
    private Integer year;
    private BigDecimal monthRent;
    private String normalFlag;
    private String yearMonth;

    public ContractPeriodVO() {
    }

    public ContractPeriodVO(Integer number, Date periodStart, Date periodEnd, Integer year,
                            BigDecimal monthRent, String normalFlag, String yearMonth) {
        this.number = number;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.year = year;
        this.monthRent = monthRent;
        this.normalFlag = normalFlag;
        this.yearMonth = yearMonth;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public Date getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(Date periodStart) {
        this.periodStart = periodStart;
    }

    public Date getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(Date periodEnd) {
        this.periodEnd = periodEnd;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public BigDecimal getMonthRent() {
        return monthRent;
    }

    public void setMonthRent(BigDecimal monthRent) {
        this.monthRent = monthRent;
    }

    public String getNormalFlag() {
        return normalFlag;
    }

    public void setNormalFlag(String normalFlag) {
        this.normalFlag = normalFlag;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    @Override
    public String toString() {
        return "ContractPeriodVO{" +
                "number=" + number +
                ", periodStart=" + periodStart +
                ", periodEnd=" + periodEnd +
                ", year=" + year +
                ", monthRent=" + monthRent +
                ", normalFlag='" + normalFlag + '\'' +
                ", yearMonth='" + yearMonth + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ContractPeriodVO that)) {
            return false;
        }
        return Objects.equals(number, that.number)
                && Objects.equals(periodStart, that.periodStart)
                && Objects.equals(periodEnd, that.periodEnd)
                && Objects.equals(year, that.year)
                && Objects.equals(monthRent, that.monthRent)
                && Objects.equals(normalFlag, that.normalFlag)
                && Objects.equals(yearMonth, that.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number, periodStart, periodEnd, year, monthRent, normalFlag, yearMonth);
    }
}
