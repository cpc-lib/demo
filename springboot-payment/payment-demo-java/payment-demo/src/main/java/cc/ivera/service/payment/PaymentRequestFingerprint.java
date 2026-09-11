package cc.ivera.service.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class PaymentRequestFingerprint {

    private PaymentRequestFingerprint() {
    }

    public static String sha256(
            String orderNo,
            Long paymentAppId,
            String channel,
            String protocol,
            Long requestAmount
    ) {
        String canonical = normalize(orderNo) + "|"
                + paymentAppId + "|"
                + normalize(channel).toUpperCase(Locale.ROOT) + "|"
                + normalize(protocol).toUpperCase(Locale.ROOT) + "|"
                + requestAmount;
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                result.append(String.format("%02x", value & 0xff));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
