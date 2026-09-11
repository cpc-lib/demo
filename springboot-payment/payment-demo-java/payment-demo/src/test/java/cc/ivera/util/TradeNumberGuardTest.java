package cc.ivera.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TradeNumberGuardTest {

    @Test
    void publicMoneyAcceptsOnlyNonNegativeJavascriptSafeIntegers() {
        assertEquals(0L, TradeNumberGuard.requireMoney(0L, "amount"));
        assertEquals(TradeNumberGuard.MAX_PUBLIC_INTEGER,
                TradeNumberGuard.requireMoney(TradeNumberGuard.MAX_PUBLIC_INTEGER, "amount"));

        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.requireMoney(-1L, "amount"));
        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.requireMoney(TradeNumberGuard.MAX_PUBLIC_INTEGER + 1L, "amount"));
    }

    @Test
    void moneyArithmeticRejectsOverflowAndNegativeBalances() {
        assertEquals(9990L, TradeNumberGuard.multiplyMoney(999L, 10));
        assertEquals(300L, TradeNumberGuard.addMoney(100L, 200L));
        assertEquals(40L, TradeNumberGuard.subtractMoney(100L, 60L));

        assertThrows(ArithmeticException.class,
                () -> TradeNumberGuard.multiplyMoney(TradeNumberGuard.MAX_PUBLIC_INTEGER, Integer.MAX_VALUE));
        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.addMoney(TradeNumberGuard.MAX_PUBLIC_INTEGER, 1L));
        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.subtractMoney(10L, 11L));
    }

    @Test
    void quantityAndStockArithmeticRejectZeroNegativeAndIntOverflow() {
        assertEquals(1, TradeNumberGuard.requirePositiveQuantity(1, "quantity"));
        assertEquals(12, TradeNumberGuard.addStock(10, 2));

        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.requirePositiveQuantity(0, "quantity"));
        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.requirePositiveQuantity(-1, "quantity"));
        assertThrows(IllegalArgumentException.class,
                () -> TradeNumberGuard.addStock(1, -2));
        assertThrows(ArithmeticException.class,
                () -> TradeNumberGuard.addStock(Integer.MAX_VALUE, 1));
    }
}
