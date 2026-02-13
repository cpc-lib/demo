package cc.ivera.ratelimit.core.resolver;

import java.lang.reflect.Method;

public interface KeyResolver {
    String resolve(String expr, Object target, Method method, Object[] args);
}
