package cc.ivera.model.bo;

import lombok.Data;

import java.util.Date;

@Data
public class BillCycleBo {
    private Date startTime;
    private Date endTime;
    private String wholeFlag;
}
