package cc.ivera.test.mapstruct.bo;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class UserDetailBO {

    private Integer userId;

//    public Integer getUserId() {
//        return userId;
//    }
//
//    public UserDetailBO setUserId(Integer userId) {
//        this.userId = userId;
//        return this;
//    }

}
