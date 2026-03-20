package cc.ivera.model.pojo.itextpdf;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @author pdai
 */
@Builder
@Data
public class User implements Serializable {

    /**
     * user id.
     */
    private Long id;

    /**
     * username.
     */
    private String userName;

    /**
     * email.
     */
    private String email;

    /**
     * phoneNumber.
     */
    private long phoneNumber;

    /**
     * description.
     */
    private String description;


}
