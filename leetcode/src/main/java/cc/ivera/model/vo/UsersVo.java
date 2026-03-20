package cc.ivera.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UsersVo implements Serializable {
    private String id;
    private String name;
    private Integer age;
    private String position;
    @JsonProperty("addtime")
    private Date addTime;
}
