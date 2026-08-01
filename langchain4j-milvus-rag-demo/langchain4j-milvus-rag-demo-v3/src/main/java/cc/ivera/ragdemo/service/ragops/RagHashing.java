package cc.ivera.ragdemo.service.ragops;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class RagHashing {

    private RagHashing() {
    }

    public static String sha256Hex(String content) {
        return sha256Hex(content == null ? new byte[0] : content.getBytes(StandardCharsets.UTF_8));
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes == null ? new byte[0] : bytes));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to calculate SHA-256", e);
        }
    }

    public static String ingestionIdempotencyKey(Long knowledgeBaseId, String documentName, String fileHash) {
        return "kb:%s:doc:%s:hash:%s".formatted(
                knowledgeBaseId == null ? 0 : knowledgeBaseId,
                documentName == null ? "" : documentName,
                fileHash == null ? "" : fileHash
        );
    }
}
