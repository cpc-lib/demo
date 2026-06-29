package com.example.vocab.config;

import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class LangChain4jConfig {
    @Bean
    @ConditionalOnExpression("'${app.llm.enabled:true}' == 'true' && '${app.llm.api-key:}' != ''")
    public OpenAiChatModel openAiChatModel(LlmProperties props) {
        return OpenAiChatModel.builder()
                .apiKey(props.getApiKey())
                .baseUrl(props.getBaseUrl())
                .modelName(props.getModel())
                .temperature(props.getTemperature())
                .timeout(Duration.ofSeconds(props.getTimeoutSeconds()))
                .logRequests(false)
                .logResponses(false)
                .build();
    }
}
