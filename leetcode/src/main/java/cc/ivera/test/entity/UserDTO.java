package cc.ivera.test.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO implements Serializable {

    private String id;

    private String name;

    private Integer age;

    private String gender;

    private Integer score;

    @JsonIgnore
    private Date dob_dto;

    private Long dob;


    public void setDob() {
        Date dobDto = this.dob_dto;
        Long timeStamp = (Long) dobDto.getTime() / 1000;
        this.dob = timeStamp;
    }


}


