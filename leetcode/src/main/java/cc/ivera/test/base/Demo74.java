package cc.ivera.test.base;


import org.junit.Test;
import cc.ivera.test.entity.UserDTO;
import cc.ivera.util.DateUtil;
import cc.ivera.util.JacksonUtil;

import java.util.Date;

//jackson 序列化的使用

public class Demo74 {

    @Test
    public void test() {
        UserDTO user = new UserDTO();
        user.setId("1024");
        user.setAge(23);
        Date date = DateUtil.dateTime(DateUtil.YYYY_MM_DD_HH_MM_SS, "1997-06-06 11:10:00");
        user.setName("hjm");
        user.setGender("male");
        user.setDob_dto(date);
        user.setScore(147);
        user.setDob();
        String jsonStr = JacksonUtil.parseObjToJson(user);
        System.out.println(jsonStr);
    }

}
