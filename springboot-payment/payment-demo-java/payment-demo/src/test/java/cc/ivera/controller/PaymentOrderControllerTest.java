package cc.ivera.controller;

import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentDisposition;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.handler.GlobalExceptionHandler;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.payment.PaymentAttemptService;
import cc.ivera.service.payment.PaymentInitiationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentOrderControllerTest {

    private PaymentInitiationService initiationService;
    private PaymentAttemptService attemptService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        initiationService = mock(PaymentInitiationService.class);
        attemptService = mock(PaymentAttemptService.class);
        mvc = MockMvcBuilders.standaloneSetup(
                        new PaymentOrderController(initiationService, attemptService)
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void userCreatesAnAttemptWithStableRequestIdAndReceivesPaymentParameters() throws Exception {
        AuthUser user = new AuthUser(7L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        PaymentOrder payment = payment();
        when(initiationService.initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user))
                .thenReturn(payment);

        mvc.perform(post("/api/order-info/ORDER-1/payments")
                        .with(request -> {
                            request.setRemoteAddr("127.0.0.1");
                            return request;
                        })
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentAppId\":9,\"protocol\":\"V3\",\"requestId\":\"request-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.paymentNo").value("PAY-1"))
                .andExpect(jsonPath("$.data.orderNo").value("ORDER-1"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentParams.codeUrl").value("wx://code"));

        verify(initiationService).initiate(
                "ORDER-1", 9L, "V3", "request-1", "127.0.0.1", user);
    }

    @Test
    void adminCannotCreateAUserPaymentAttempt() throws Exception {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));

        mvc.perform(post("/api/order-info/ORDER-1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentAppId\":9,\"protocol\":\"V3\",\"requestId\":\"request-1\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));

        verify(initiationService, never()).initiate(
                any(), any(), any(), any(), any(), any());
    }

    @Test
    void userOrAdminMayListAuthorizedPaymentAttempts() throws Exception {
        AuthUser admin = new AuthUser(1L, "admin", UserRole.ADMIN);
        AuthContext.setUser(admin);
        when(attemptService.listForOrder("ORDER-1", admin))
                .thenReturn(Collections.singletonList(payment()));

        mvc.perform(get("/api/order-info/ORDER-1/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].paymentNo").value("PAY-1"))
                .andExpect(jsonPath("$.data[0].disposition").value("NONE"));

        verify(attemptService).listForOrder("ORDER-1", admin);
    }

    @Test
    void invalidPaymentRequestIsRejectedBeforeBusinessExecution() throws Exception {
        AuthContext.setUser(new AuthUser(7L, "alice", UserRole.USER));

        mvc.perform(post("/api/order-info/ORDER-1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentAppId\":9,\"protocol\":\"\",\"requestId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private PaymentOrder payment() {
        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo("PAY-1");
        payment.setOrderNo("ORDER-1");
        payment.setPaymentAppId(9L);
        payment.setChannel("WXPAY");
        payment.setProtocol("V3");
        payment.setRequestAmount(1000L);
        payment.setStatus(PaymentOrderStatus.PENDING);
        payment.setDisposition(PaymentDisposition.NONE);
        payment.setPaymentParams("{\"codeUrl\":\"wx://code\"}");
        payment.setExpiresAt(new Date(System.currentTimeMillis() + 120_000L));
        return payment;
    }
}
