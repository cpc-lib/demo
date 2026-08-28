package cc.ivera.ratelimit.core.resolver;

import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.ConcurrentReferenceHashMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.Map;

public class SpelKeyResolver implements KeyResolver {

    private final ExpressionParser parser = new SpelExpressionParser();
    private final Map<String, Expression> cache = new ConcurrentReferenceHashMap<String, Expression>();
    private final DefaultParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Override
    public String resolve(String expr, Object target, Method method, Object[] args) {
        Expression e = cache.get(expr);
        if (e == null) {
            e = parser.parseExpression(expr);
            cache.put(expr, e);
        }

        HttpServletRequest request = null;
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes) {
            request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        }

        StandardEvaluationContext ctx = new StandardEvaluationContext();
        SpelRootObject root = new SpelRootObject(target, method, args, request);
        ctx.setRootObject(root);

        ctx.setVariable("target", target);
        ctx.setVariable("method", method);
        ctx.setVariable("methodName", root.getMethodName());
        ctx.setVariable("args", args);
        ctx.setVariable("request", request);

        String[] paramNames = nameDiscoverer.getParameterNames(method);
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                ctx.setVariable(paramNames[i], args[i]);
            }
        }

        Object val = e.getValue(ctx);
        return val == null ? "" : String.valueOf(val);
    }
}
