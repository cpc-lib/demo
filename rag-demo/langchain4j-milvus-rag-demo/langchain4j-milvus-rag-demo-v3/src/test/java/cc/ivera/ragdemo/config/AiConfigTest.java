package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.service.tenant.DynamicModelFactory;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AiConfigTest {

    @Test
    void chatModelDelegatesToolCallsToDynamicTenantModel() {
        DynamicModelFactory modelFactory = mock(DynamicModelFactory.class);
        RecordingChatModel tenantModel = new RecordingChatModel();
        when(modelFactory.getLlmModel()).thenReturn(tenantModel);
        AiConfig aiConfig = new AiConfig(new RagProperties(), modelFactory);
        ChatLanguageModel chatModel = aiConfig.chatModel();
        List<ChatMessage> messages = List.of(UserMessage.from("明天上海天气如何"));
        List<ToolSpecification> tools = List.of(ToolSpecification.builder()
                .name("weatherForecast")
                .description("查询天气")
                .build());

        Response<AiMessage> response = chatModel.generate(messages, tools);

        assertThat(response.content().text()).isEqualTo("tool-ready");
        assertThat(tenantModel.receivedMessages).isSameAs(messages);
        assertThat(tenantModel.receivedTools).isSameAs(tools);
    }

    private static class RecordingChatModel implements ChatLanguageModel {

        private List<ChatMessage> receivedMessages;
        private List<ToolSpecification> receivedTools;

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages) {
            this.receivedMessages = messages;
            return Response.from(AiMessage.from("plain"));
        }

        @Override
        public Response<AiMessage> generate(List<ChatMessage> messages, List<ToolSpecification> toolSpecifications) {
            this.receivedMessages = messages;
            this.receivedTools = toolSpecifications;
            return Response.from(AiMessage.from("tool-ready"));
        }
    }
}
