package cc.ivera.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TimePeriodDTO implements Serializable {
    private Date startTime;
    private Date endTime;
}
