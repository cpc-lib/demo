package com.example.vocab.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.llm")
public class LlmProperties {
    /** true: use LangChain4j OpenAI-compatible API; false: use deterministic local fallback. */
    private boolean enabled = true;
    private String apiKey = "";
    /** OpenAI-compatible base URL, e.g. https://api.openai.com/v1 or https://dashscope.aliyuncs.com/compatible-mode/v1 */
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-4o-mini";
    private double temperature = 0.2;
    private int timeoutSeconds = 45;
}
