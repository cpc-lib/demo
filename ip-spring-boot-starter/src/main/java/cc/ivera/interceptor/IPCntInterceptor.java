package cc.ivera.interceptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import cc.ivera.service.IPCntService;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//@Component("iPCntInterceptor") 此处注解外部无法扫描，不能生效
public class IPCntInterceptor implements HandlerInterceptor {

    @Autowired
    private IPCntService ipCntService;//外部应用在autoConfiguration中可以扫描到此bean，挂载成功

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ipCntService.count();
        return true;
    }
}
