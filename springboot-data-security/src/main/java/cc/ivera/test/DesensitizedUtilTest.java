package cc.ivera.test;

import cn.hutool.core.util.DesensitizedUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DesensitizedUtilTest {

    private static Logger logger=LoggerFactory.getLogger(DesensitizedUtilTest.class);
    public static void main(String[] args) {
        //身份证
        String idCardNum = DesensitizedUtil.idCardNum("51343620000320711X", 1, 2);
        logger.info("idCardNum:{}",idCardNum);

        //电话
        String mobilePhone = DesensitizedUtil.mobilePhone("18049531999");
        logger.info("mobilePhone:{}",mobilePhone);

        //密码
        String password = DesensitizedUtil.password("1234567890");
        logger.info("password:{}",password);
    }
}

