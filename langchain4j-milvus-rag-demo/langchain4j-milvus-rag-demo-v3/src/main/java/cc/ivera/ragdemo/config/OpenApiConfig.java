package cc.ivera.ragdemo.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ragOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("LangChain4j Milvus RAG Demo API")
                        .version("1.1.0")
                        .description("RAG management APIs for chat, ingestion, knowledge bases, chunks, query audit, multimodal assets, and Milvus operations.")
                        .license(new License().name("Demo project")));
    }
}
