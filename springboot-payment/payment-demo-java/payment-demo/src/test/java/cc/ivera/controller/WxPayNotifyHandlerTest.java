package cc.ivera.controller;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.support.WxPayNotifyHandler;
import cc.ivera.exception.ConflictException;
import com.wechat.pay.contrib.apache.httpclient.auth.Verifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WxPayNotifyHandlerTest {

    private Verifier verifier;
    private Consumer<Map<String, Object>> processor;
    private WxPayNotifyHandler handler;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        verifier = mock(Verifier.class);
        processor = mock(Consumer.class);
        handler = new WxPayNotifyHandler(verifier, mock(WxPayConfig.class), mock(PaymentConfigLoader.class));
    }

    @Test
    void invalidSignatureNeverReachesProcessor() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        String body = handler.handle(request(), response, processor, "支付通知失败");

        assertEquals(500, response.getStatus());
        assertTrue(body.contains("ERROR"));
        verify(verifier).verify(anyString(), any(byte[].class), anyString());
        verifyNoInteractions(processor);
    }

    @Test
    void validRepeatedNotificationsAreVerifiedAndReachProcessorEachTime() {
        when(verifier.verify(anyString(), any(byte[].class), anyString())).thenReturn(true);
        doAnswer(invocation -> {
            Map<String, Object> body = invocation.getArgument(0);
            assertEquals("notify-1", body.get("id"));
            return null;
        }).when(processor).accept(any());

        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = new MockHttpServletResponse();
            String body = handler.handle(request(), response, processor, "支付通知失败");
            assertEquals(200, response.getStatus());
            assertTrue(body.contains("SUCCESS"));
        }

        org.mockito.InOrder calls = org.mockito.Mockito.inOrder(verifier, processor);
        calls.verify(verifier).verify(anyString(), any(byte[].class), anyString());
        calls.verify(processor).accept(any());
        calls.verify(verifier).verify(anyString(), any(byte[].class), anyString());
        calls.verify(processor).accept(any());
        verify(processor, times(2)).accept(any());
    }

    @Test
    void busyProcessorReturnsHttp500ForRetry() {
        assertProcessorFailure(new ConflictException("通知正在处理"));
    }

    @Test
    void failedProcessorReturnsHttp500ForRetry() {
        assertProcessorFailure(new IllegalStateException("业务事务失败"));
    }

    private void assertProcessorFailure(RuntimeException failure) {
        when(verifier.verify(anyString(), any(byte[].class), anyString())).thenReturn(true);
        doThrow(failure).when(processor).accept(any());
        MockHttpServletResponse response = new MockHttpServletResponse();

        String body = handler.handle(request(), response, processor, "支付通知失败");

        assertEquals(500, response.getStatus());
        assertTrue(body.contains("ERROR"));
        verify(processor).accept(any());
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("{\"id\":\"notify-1\"}".getBytes(StandardCharsets.UTF_8));
        request.addHeader("Wechatpay-Serial", "serial-1");
        request.addHeader("Wechatpay-Signature", "signature-1");
        request.addHeader("Wechatpay-Nonce", "nonce-1");
        request.addHeader("Wechatpay-Timestamp", Long.toString(Instant.now().getEpochSecond()));
        return request;
    }
}
