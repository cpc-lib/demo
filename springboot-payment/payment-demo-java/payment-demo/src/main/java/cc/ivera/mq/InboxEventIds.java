package cc.ivera.mq;

import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class InboxEventIds {

    private InboxEventIds() {
    }

    public static String resolve(String preferredId, String stableIdentity) {
        if (StringUtils.hasText(preferredId) && preferredId.trim().length() <= 36) {
            return preferredId.trim();
        }
        return UUID.nameUUIDFromBytes(stableIdentity.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
