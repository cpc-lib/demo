package cc.ivera.cache.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void returnsJsonForResponseStatusExceptionEvenWhenBrowserRequestsHtml() throws Exception {
        mockMvc.perform(get("/test/orders/404").accept(MediaType.TEXT_HTML))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("order not found: 404"))
                .andExpect(jsonPath("$.path").value("/test/orders/404"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void returnsJsonForMethodArgumentTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/orders/not-a-number").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value("invalid value for parameter 'id': not-a-number"))
                .andExpect(jsonPath("$.path").value("/test/orders/not-a-number"));
    }

    @Test
    void returnsJsonForUnexpectedExceptions() throws Exception {
        mockMvc.perform(get("/test/fail").accept(MediaType.TEXT_HTML))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.error").value("Internal Server Error"))
                .andExpect(jsonPath("$.message").value("internal server error"))
                .andExpect(jsonPath("$.path").value("/test/fail"));
    }

    @RestController
    static class TestController {

        @GetMapping("/test/orders/{id}")
        String getOrder(@PathVariable Long id) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "order not found: " + id);
        }

        @GetMapping("/test/fail")
        String fail() {
            throw new IllegalStateException("boom");
        }
    }
}
