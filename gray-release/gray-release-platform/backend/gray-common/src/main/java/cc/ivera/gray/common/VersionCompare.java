package cc.ivera.gray.common;

public final class VersionCompare {
    private VersionCompare() {
    }

    public static int compare(String left, String right) {
        if (left == null && right == null) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        if (right == null) {
            return 1;
        }
        String[] leftParts = left.split("\\.");
        String[] rightParts = right.split("\\.");
        int size = Math.max(leftParts.length, rightParts.length);
        for (int i = 0; i < size; i++) {
            int leftValue = i < leftParts.length ? parse(leftParts[i]) : 0;
            int rightValue = i < rightParts.length ? parse(rightParts[i]) : 0;
            if (leftValue != rightValue) {
                return Integer.compare(leftValue, rightValue);
            }
        }
        return 0;
    }

    private static int parse(String value) {
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return 0;
        }
        return Integer.parseInt(digits);
    }
}

