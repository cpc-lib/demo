package cc.ivera.model.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class PaymentRule {
    private Date startTime;//开始时间
    private Date endTime;//结束时间
    private Integer months;//前n月不递增(免租期后计算,还是以合同开始时间)
    private BigDecimal ratio;//递增比率
    private Integer jumpYear;//每n年递增一次
    private Integer noChangeYear;//后n年不递增
    private BigDecimal initValue;//初始月租金
    private Integer freeDays;//免租期天数


    //1.get the free day end
    //2.get the first no change node start time
    //3.check time gap

}
