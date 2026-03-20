package cc.ivera.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
public class ContractVo implements Serializable {
    private int number;
    private Date periodStart;
    private Date periodEnd;
    private int year;
}
