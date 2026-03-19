package cc.ivera.ftp.util;

public final class ConfigSupport {

    private ConfigSupport() {
    }

    public static String getString(String propertyName, String envName, String defaultValue) {
        String value = getOptionalValue(propertyName, envName);
        return value == null ? defaultValue : value;
    }

    public static int getInt(String propertyName, String envName, int defaultValue) {
        String value = getOptionalValue(propertyName, envName);
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid integer for " + propertyName + "/" + envName + ": " + value, e);
        }
    }

    private static String getOptionalValue(String propertyName, String envName) {
        String value = trimToNull(System.getProperty(propertyName));
        if (value != null) {
            return value;
        }
        return trimToNull(System.getenv(envName));
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
