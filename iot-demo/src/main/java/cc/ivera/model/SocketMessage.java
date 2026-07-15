package cc.ivera.model;

import java.time.Instant;

public record SocketMessage<T>(String type, T data, Instant sentAt) {
    public static <T> SocketMessage<T> of(String type, T data) {
        return new SocketMessage<>(type, data, Instant.now());
    }
}
