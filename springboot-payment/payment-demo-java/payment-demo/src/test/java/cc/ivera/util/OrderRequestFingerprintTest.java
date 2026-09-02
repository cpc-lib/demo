package cc.ivera.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class OrderRequestFingerprintTest {

    @Test
    void cartFingerprintIsStableSha256AndChangesWithPaymentApp() {
        assertEquals(
                "59c95d4442512ddf1b5f88e007778060b43e5ab64d7e2f472adba89581b7b40d",
                OrderRequestFingerprint.cartCheckout(9L)
        );
        assertNotEquals(
                OrderRequestFingerprint.cartCheckout(9L),
                OrderRequestFingerprint.cartCheckout(10L)
        );
    }

    @Test
    void directFingerprintNormalizesChannelAndIncludesProductQuantityAndApp() {
        String fingerprint = OrderRequestFingerprint.directBuy(101L, 1, 9L, " wxpay ");

        assertEquals(64, fingerprint.length());
        assertEquals(fingerprint, OrderRequestFingerprint.directBuy(101L, 1, 9L, "WXPAY"));
        assertNotEquals(fingerprint, OrderRequestFingerprint.directBuy(102L, 1, 9L, "WXPAY"));
        assertNotEquals(fingerprint, OrderRequestFingerprint.directBuy(101L, 2, 9L, "WXPAY"));
        assertNotEquals(fingerprint, OrderRequestFingerprint.directBuy(101L, 1, 10L, "WXPAY"));
    }
}
