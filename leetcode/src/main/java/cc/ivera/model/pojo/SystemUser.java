package cc.ivera.model.pojo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/30/2021 8:05 PM
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SystemUser implements Serializable {

    private String id;

    private String name;

    private Integer age;

    private String gender;

    private Integer score;

    private Date dob;

}
