package com.example.vocab.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class DatasourceUrlEncodingTest {
    @Test
    void defaultJdbcUrlUsesJavaSupportedCharacterEncoding() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml resource").isNotNull();
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml)
                    .doesNotContain("characterEncoding=utf8mb4")
                    .contains("characterEncoding=UTF-8");
        }
    }

    @Test
    void llmSupportIsEnabledByDefaultAndCanBeDisabledByEnvironment() throws IOException {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("application.yml")) {
            assertThat(in).as("application.yml resource").isNotNull();
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);

            assertThat(yaml).contains("enabled: ${LLM_ENABLED:true}");
        }
    }
}
