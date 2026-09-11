package cc.ivera.util;

public final class TradeNumberGuard {

    public static final long MAX_PUBLIC_INTEGER = 9_007_199_254_740_991L;

    private TradeNumberGuard() {
    }

    public static long requireMoney(long value, String field) {
        if (value < 0 || value > MAX_PUBLIC_INTEGER) {
            throw new IllegalArgumentException(field + "必须在0到" + MAX_PUBLIC_INTEGER + "分之间");
        }
        return value;
    }

    public static int requirePositiveQuantity(int value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + "必须大于0");
        }
        return value;
    }

    public static long addMoney(long left, long right) {
        requireMoney(left, "left");
        requireMoney(right, "right");
        return requireMoney(Math.addExact(left, right), "amount");
    }

    public static long subtractMoney(long minuend, long subtrahend) {
        requireMoney(minuend, "minuend");
        requireMoney(subtrahend, "subtrahend");
        return requireMoney(Math.subtractExact(minuend, subtrahend), "amount");
    }

    public static long multiplyMoney(long unitAmount, int quantity) {
        requireMoney(unitAmount, "unitAmount");
        requirePositiveQuantity(quantity, "quantity");
        return requireMoney(Math.multiplyExact(unitAmount, (long) quantity), "amount");
    }

    public static int addStock(int current, int delta) {
        if (current < 0) {
            throw new IllegalArgumentException("currentStock不能小于0");
        }
        int result = Math.addExact(current, delta);
        if (result < 0) {
            throw new IllegalArgumentException("库存调整后不能小于0");
        }
        return result;
    }
}
