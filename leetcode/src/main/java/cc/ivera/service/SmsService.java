package cc.ivera.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import cc.ivera.constant.Constants;
import cc.ivera.exception.BusinessException;
import cc.ivera.model.vo.ErrorResult;
import cc.ivera.template.SmsTemplate;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

//TODO.md 将短信存放到redis中，并设置过期时间。

//TODO.md 限制访问短信接口的次数

//TODO.md 短信验证通过登录的的角色查询与权限查询。

//TODO.md 使用shiro框架实现

//TODO.md 接入微信登录

//TODO.md 接入邮箱登录


@Service
public class SmsService {

    @Autowired
    private SmsTemplate smsTemplate;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    public void sendVerificationCode(String phone) {
        Random random = new Random();
        String[] str = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9"};
        StringBuilder strBuilder = new StringBuilder();
        for (int i = 0; i < Constants.CODE_LEN; i++) {
            strBuilder.append(str[random.nextInt(str.length)]);
        }
        String code = strBuilder.toString();
        smsTemplate.sendverificationCode(phone, code);
        redisTemplate.opsForValue().set("VERIFICATION_CODE_" + phone, code, Duration.ofMinutes(5));
    }

    public boolean checkVerificationCode(String phone, String code) {
        String codeInRedis = redisTemplate.opsForValue().get("VERIFICATION_CODE_" + phone);
        if (StringUtils.isEmpty(codeInRedis) || !codeInRedis.equals(code)) {
            //验证码无效
            //根据不同的响应设置创建不同的ErrorResult对象
            //抛出异常，即使处理（try...catch处理能够继续执行），
            //交给springmvc统一处理异常确保系统能够一直运行。
            //异常，程序中断，方法无返回值但系统仍可运行
            throw new BusinessException(ErrorResult.loginError());
            //结束
        }
        redisTemplate.delete("VERIFICATION_CODE_" + phone);
        return true;
    }

    public Map login(String phone, String code) {
        Map map = new HashMap();
        if (checkVerificationCode(phone, code)) {
            map.put("code", 200);
            map.put("message", "");
            map.put("data", new ArrayList<>());
            return map;
        } else {
            map.put("coe", 2001);
            map.put("message", "手机号码错误或者验证码错误");
            map.put("data", new ArrayList<>());
            return map;
        }

    }


}
