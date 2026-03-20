package cc.ivera.model.dto;

import lombok.Data;

import javax.xml.bind.annotation.XmlRootElement;

@Data
//@XStreamAlias("UserDTO")
@XmlRootElement(name = "UserDTO")
public class UserXmlDTO {
    private Integer id;
    private String userName;
    private Integer userAge;
}
