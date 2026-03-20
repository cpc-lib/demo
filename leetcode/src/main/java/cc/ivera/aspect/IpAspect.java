package cc.ivera.aspect;

import com.github.hiwepy.ip2region.spring.boot.IP2regionTemplate;
import com.github.hiwepy.ip2region.spring.boot.ext.RegionAddress;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cc.ivera.util.HttpContextUtil;
import cc.ivera.util.WebUtil;

import javax.servlet.http.HttpServletRequest;
import java.text.MessageFormat;

@Aspect
@Component
public class IpAspect {

    @Autowired
    IP2regionTemplate template;

    @Pointcut("@annotation(cc.ivera.annotation.Ip)")
    public void pointcut() {
    }

    @Around("pointcut()")
    public Object doAround(ProceedingJoinPoint point) throws Throwable {
        HttpServletRequest request = HttpContextUtil.getHttpServletRequest();
        String ip = WebUtil.getIpAddress(request);
        RegionAddress regionAddress = template.getRegionAddress(ip);
        System.out.println(MessageFormat.format("当前IP为:[{0}]；当前IP地址解析出来的地址为:[{1}]", ip, regionAddress.getCity()));
        return point.proceed();
    }

}

