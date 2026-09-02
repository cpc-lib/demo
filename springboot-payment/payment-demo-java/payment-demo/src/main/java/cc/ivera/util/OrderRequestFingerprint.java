package cc.ivera.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

public final class OrderRequestFingerprint {

    private OrderRequestFingerprint() {
    }

    public static String cartCheckout(Long paymentAppId) {
        return sha256("CART_CHECKOUT|" + paymentAppId);
    }

    public static String directBuy(
            Long productId,
            int quantity,
            Long paymentAppId,
            String channelCode
    ) {
        String normalizedChannel = channelCode == null
                ? ""
                : channelCode.trim().toUpperCase(Locale.ROOT);
        return sha256("DIRECT_BUY|" + productId + "|" + quantity + "|"
                + paymentAppId + "|" + normalizedChannel);
    }

    private static String sha256(String canonicalRequest) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(canonicalRequest.getBytes(StandardCharsets.UTF_8));
            StringBuilder value = new StringBuilder(digest.length * 2);
            for (byte current : digest) {
                value.append(String.format("%02x", current & 0xff));
            }
            return value.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is unavailable", ex);
        }
    }
}
