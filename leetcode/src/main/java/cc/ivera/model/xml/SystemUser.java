package cc.ivera.model.xml;

import lombok.Data;

import java.util.Date;
import java.util.List;


@Data
public class SystemUser {
    private String phoneNum;
    private String name;
    private String uid;
    private String email;
    private Date dob;
    private CurrentRole role;
    private List<Hobby> hobbies;
    private List<CurrentRole> roles;
}
