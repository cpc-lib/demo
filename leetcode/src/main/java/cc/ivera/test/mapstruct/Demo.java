package cc.ivera.test.mapstruct;

import org.junit.Test;
import cc.ivera.test.mapstruct.bo.UserBO;
import cc.ivera.test.mapstruct.bo.UserDetailBO;
import cc.ivera.test.mapstruct.convert.UserConvert;
import cc.ivera.test.mapstruct.dataobject.UserDO;

public class Demo {

    @Test
    public void test1() {
        // 创建 UserDO 对象
        UserDO userDO = new UserDO()
                .setId(1).setUsername("admin123").setPassword("@12345678a");
        // 进行转换
        UserBO userBO = UserConvert.INSTANCE.convert(userDO);
        System.out.println(userBO.getId());
        System.out.println(userBO.getUsername());
        System.out.println(userBO.getPassword());
    }

    @Test
    public void test2() {
        // 创建 UserDO 对象
        UserDO userDO = new UserDO()
                .setId(1).setUsername("admin123").setPassword("@12345678a");
        // 进行转换
        UserDetailBO userDetailBO = UserConvert.INSTANCE.convertDetail(userDO);
        System.out.println(userDetailBO.getUserId());
    }
}
