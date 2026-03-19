package cc.ivera.cache.sentinel;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class OrderQueryRateLimitSupportTest {

    @Test
    void resolvesFirstForwardedIp() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Forwarded-For", "unknown, 203.0.113.10, 10.0.0.9");
        request.setRemoteAddr("127.0.0.1");

        assertThat(OrderQueryRateLimitSupport.resolveClientIp(request)).isEqualTo("203.0.113.10");
    }

    @Test
    void fallsBackToRemoteAddrWhenForwardHeadersMissing() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertThat(OrderQueryRateLimitSupport.resolveClientIp(request)).isEqualTo("127.0.0.1");
    }

    @Test
    void buildsCompositeLimitKeyFromIpAndOrderId() {
        assertThat(OrderQueryRateLimitSupport.buildLimitKey(" 203.0.113.10 ", 42L)).isEqualTo("203.0.113.10:42");
    }
}
