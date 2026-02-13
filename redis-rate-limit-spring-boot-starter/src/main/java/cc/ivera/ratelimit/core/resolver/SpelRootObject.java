package cc.ivera.ratelimit.core.resolver;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;

public class SpelRootObject {
    private final Object target;
    private final Method method;
    private final Object[] args;
    private final HttpServletRequest request;

    public SpelRootObject(Object target, Method method, Object[] args, HttpServletRequest request) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.request = request;
    }

    public Object getTarget() { return target; }
    public Method getMethod() { return method; }
    public Object[] getArgs() { return args; }
    public HttpServletRequest getRequest() { return request; }

    public String getMethodName() { return method == null ? "unknown" : method.getName(); }
}
