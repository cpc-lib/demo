package cc.ivera.ragdemo.service.ragops;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SqlCreateTableStatementExtractor {

    private SqlCreateTableStatementExtractor() {
    }

    public static List<String> extract(String sql) {
        if (sql == null || sql.isBlank()) {
            return List.of();
        }
        List<String> statements = new ArrayList<>();
        for (String rawStatement : sql.split(";")) {
            String statement = stripCommentLines(rawStatement).trim();
            if (!startsWithCreateTable(statement)) {
                continue;
            }
            statements.add(normalizeCreateTable(statement));
        }
        return List.copyOf(statements);
    }

    private static String stripCommentLines(String rawStatement) {
        StringBuilder cleaned = new StringBuilder();
        for (String line : rawStatement.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("--") || startsWithIgnoreCase(trimmed, "DELIMITER ")) {
                continue;
            }
            if (!cleaned.isEmpty()) {
                cleaned.append(System.lineSeparator());
            }
            cleaned.append(line);
        }
        return cleaned.toString();
    }

    private static boolean startsWithCreateTable(String statement) {
        return startsWithIgnoreCase(statement, "CREATE TABLE ");
    }

    private static String normalizeCreateTable(String statement) {
        String normalized = statement.trim();
        if (startsWithIgnoreCase(normalized, "CREATE TABLE IF NOT EXISTS ")) {
            return normalized;
        }
        return normalized.replaceFirst("(?i)^CREATE\\s+TABLE\\s+", "CREATE TABLE IF NOT EXISTS ");
    }

    private static boolean startsWithIgnoreCase(String value, String prefix) {
        return value.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT));
    }
}
