package cc.ivera.characterization;

import cc.ivera.config.AuthProperties;
import cc.ivera.config.WebMvcConfig;
import cc.ivera.controller.CartController;
import cc.ivera.security.AdminInterceptor;
import cc.ivera.security.AuthInterceptor;
import cc.ivera.security.JwtTokenService;
import cc.ivera.service.CartService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CorsPreflightCharacterizationTest {

    private AnnotationConfigWebApplicationContext context;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        context.register(TestConfig.class);
        context.refresh();
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void reactCartWritePreflightReturnsOkWithCredentialCorsHeaders() throws Exception {
        mockMvc.perform(options("/api/cart/items")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, containsString("POST")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("authorization")))
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, containsString("content-type")));
    }

    @Configuration
    @EnableWebMvc
    @Import({
            WebMvcConfig.class,
            AuthInterceptor.class,
            AdminInterceptor.class,
            JwtTokenService.class,
            CartController.class
    })
    static class TestConfig {

        @Bean
        AuthProperties authProperties() {
            AuthProperties properties = new AuthProperties();
            properties.setJwtSecret("0123456789abcdef0123456789abcdef");
            properties.setAllowedOrigins(Collections.singletonList("http://localhost:3000"));
            return properties;
        }

        @Bean
        CartService cartService() {
            return mock(CartService.class);
        }
    }
}
