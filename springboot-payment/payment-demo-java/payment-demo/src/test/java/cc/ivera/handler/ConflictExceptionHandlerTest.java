package cc.ivera.handler;

import cc.ivera.exception.ConflictException;
import cc.ivera.exception.BizException;
import cc.ivera.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ConflictExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ExceptionProbeController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void conflictExceptionUsesHttp409AndExistingResponseEnvelope() throws Exception {
        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("库存不足"));
    }

    @Test
    void ordinaryBizExceptionKeepsLegacyHttp200Behavior() throws Exception {
        mockMvc.perform(get("/test/business-error"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(-1))
                .andExpect(jsonPath("$.message").value("普通业务校验失败"));
    }

    @Test
    void notFoundExceptionUsesHttp404AndExistingResponseEnvelope() throws Exception {
        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("商品不存在"));
    }

    @RestController
    static class ExceptionProbeController {

        @GetMapping("/test/conflict")
        public void conflict() {
            throw new ConflictException("库存不足");
        }

        @GetMapping("/test/business-error")
        public void businessError() {
            throw new BizException("普通业务校验失败");
        }

        @GetMapping("/test/not-found")
        public void notFound() {
            throw new NotFoundException("商品不存在");
        }
    }
}
