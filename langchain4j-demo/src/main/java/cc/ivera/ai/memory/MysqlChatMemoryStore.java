package cc.ivera.ai.memory;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MysqlChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MysqlChatMemoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);
        return jdbcTemplate.query(
                "SELECT role, content FROM chat_memory_message WHERE memory_id=? ORDER BY msg_index ASC",
                (rs, rowNum) -> {
                    String role = rs.getString("role");
                    String content = rs.getString("content");
                    return switch (role) {
                        case "SYSTEM" -> SystemMessage.from(content);
                        case "USER" -> UserMessage.from(content);
                        case "AI" -> AiMessage.from(content);
                        default -> UserMessage.from(content);
                    };
                },
                id
        );
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);

        // 简单策略：先删后写（可按需优化为增量写入）
        jdbcTemplate.update("DELETE FROM chat_memory_message WHERE memory_id=?", id);

        int idx = 0;
        for (ChatMessage msg : messages) {
            String role = roleOf(msg);
            String content = contentOf(msg);
            jdbcTemplate.update(
                    "INSERT INTO chat_memory_message(memory_id, msg_index, role, content) VALUES (?,?,?,?)",
                    id, idx++, role, content
            );
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        jdbcTemplate.update("DELETE FROM chat_memory_message WHERE memory_id=?", String.valueOf(memoryId));
    }

    private String roleOf(ChatMessage msg) {
        if (msg instanceof SystemMessage) return "SYSTEM";
        if (msg instanceof UserMessage) return "USER";
        if (msg instanceof AiMessage) return "AI";
        return "USER";
    }

    private String contentOf(ChatMessage msg) {
        if (msg instanceof SystemMessage m) return m.text();
        if (msg instanceof UserMessage m) return m.singleText();
        if (msg instanceof AiMessage m) return m.text();
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            return String.valueOf(msg);
        }
    }
}
