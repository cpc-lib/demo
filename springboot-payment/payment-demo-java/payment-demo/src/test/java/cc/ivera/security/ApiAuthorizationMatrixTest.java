package cc.ivera.security;

import cc.ivera.config.WebMvcConfig;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ApiAuthorizationMatrixTest {

    @Test
    @SuppressWarnings("unchecked")
    void routeMatrixKeepsOnlyCatalogAuthEntryAndSignedCallbacksPublic() {
        InterceptorRegistry registry = registry();
        List<Object> interceptors = (List<Object>) ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        MappedInterceptor authenticated = (MappedInterceptor) interceptors.get(0);
        AntPathMatcher pathMatcher = new AntPathMatcher();

        assertFalse(authenticated.matches("/api/product/list", pathMatcher));
        assertTrue(authenticated.matches("/api/product/test", pathMatcher));
        assertTrue(authenticated.matches("/api/admin/products", pathMatcher));
        assertTrue(authenticated.matches("/api/admin/products/7/stock-adjustments", pathMatcher));
        assertTrue(authenticated.matches("/api/admin/outbox/event-1/retry", pathMatcher));
        assertFalse(authenticated.matches("/api/auth/login", pathMatcher));
        assertFalse(authenticated.matches("/api/auth/register", pathMatcher));
        assertFalse(authenticated.matches("/api/auth/refresh", pathMatcher));
        assertFalse(authenticated.matches("/api/wx-pay/native/notify", pathMatcher));
        assertFalse(authenticated.matches("/api/wx-pay-v2/native/notify", pathMatcher));
        assertFalse(authenticated.matches("/api/ali-pay/trade/notify", pathMatcher));
        assertTrue(authenticated.matches("/api/auth/me", pathMatcher));
        assertTrue(authenticated.matches("/api/cart", pathMatcher));
        assertTrue(authenticated.matches("/api/order-info/checkout", pathMatcher));
        assertTrue(authenticated.matches("/api/order-info/idempotency-keys", pathMatcher));
        assertTrue(authenticated.matches("/api/refund-info/apply", pathMatcher));
    }

    @Test
    @SuppressWarnings("unchecked")
    void adminMatrixProtectsGlobalManagementButNotPersonalCheckoutReads() {
        InterceptorRegistry registry = registry();
        List<Object> interceptors = (List<Object>) ReflectionTestUtils.invokeMethod(registry, "getInterceptors");
        MappedInterceptor admin = (MappedInterceptor) interceptors.get(1);
        AntPathMatcher pathMatcher = new AntPathMatcher();

        assertTrue(admin.matches("/api/order-info/list", pathMatcher));
        assertTrue(admin.matches("/api/payment-config/reload", pathMatcher));
        assertTrue(admin.matches("/api/payment-channel/list", pathMatcher));
        assertTrue(admin.matches("/api/payment-app/list-all", pathMatcher));
        assertTrue(admin.matches("/api/payment-app/9/status", pathMatcher));
        assertTrue(admin.matches("/api/bill/list", pathMatcher));
        assertTrue(admin.matches("/api/reconciliation/list", pathMatcher));
        assertTrue(admin.matches("/api/refund-info/list", pathMatcher));
        assertTrue(admin.matches("/api/refund-info/approve/R1", pathMatcher));
        assertTrue(admin.matches("/api/refund-info/query/R1", pathMatcher));
        assertTrue(admin.matches("/api/admin/products", pathMatcher));
        assertTrue(admin.matches("/api/admin/products/7/stock-operations", pathMatcher));
        assertTrue(admin.matches("/api/admin/outbox/event-1/retry", pathMatcher));
        assertTrue(admin.matches("/api/wx-pay/check-order-status/ORDER-1", pathMatcher));
        assertTrue(admin.matches("/api/ali-pay/check-order-status/ORDER-1", pathMatcher));
        assertFalse(admin.matches("/api/payment-app/list", pathMatcher));
        assertFalse(admin.matches("/api/order-info/my-list", pathMatcher));
        assertFalse(admin.matches("/api/order-info/idempotency-keys", pathMatcher));
        assertFalse(admin.matches("/api/refund-info/list/ORDER-1", pathMatcher));
    }

    private InterceptorRegistry registry() {
        WebMvcConfig config = new WebMvcConfig(
                mock(AuthInterceptor.class),
                mock(AdminInterceptor.class),
                new cc.ivera.config.AuthProperties()
        );
        InterceptorRegistry registry = new InterceptorRegistry();
        config.addInterceptors(registry);
        return registry;
    }
}
