package cc.ivera.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class ContractDTO implements Serializable {
    private Integer year;
    private Integer start_year;
    private Integer end_year;
    private Integer start_month;
    private Integer end_month;
    private Integer start_day;
    private Integer end_day;

}
