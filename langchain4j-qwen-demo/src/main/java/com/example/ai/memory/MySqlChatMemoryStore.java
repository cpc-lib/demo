package com.example.ai.memory;

import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

public class MySqlChatMemoryStore implements ChatMemoryStore {

    private final JdbcTemplate jdbc;

    public MySqlChatMemoryStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
        String id = String.valueOf(memoryId);

        List<String> rows = jdbc.query(
                "SELECT messages_json FROM chat_memory WHERE memory_id = ?",
                (rs, rowNum) -> rs.getString(1),
                id
        );

        if (rows.isEmpty() || rows.get(0) == null || rows.get(0).isBlank()) {
            return Collections.emptyList();
        }

        try {
            return ChatMessageDeserializer.messagesFromJson(rows.get(0));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize chat memory", e);
        }
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String id = String.valueOf(memoryId);

        try {
            String json = ChatMessageSerializer.messagesToJson(messages);

            int updated = jdbc.update(
                    "UPDATE chat_memory SET messages_json = ? WHERE memory_id = ?",
                    json, id
            );

            if (updated == 0) {
                jdbc.update(
                        "INSERT INTO chat_memory(memory_id, messages_json) VALUES(?, ?)",
                        id, json
                );
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize chat memory", e);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
        jdbc.update("DELETE FROM chat_memory WHERE memory_id = ?", String.valueOf(memoryId));
    }
}
