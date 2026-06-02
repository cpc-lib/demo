package cc.ivera.migration.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {
    private int defaultShardSize = 50_000;
    private int defaultBatchSize = 5_000;
    private int defaultThreadSize = 16;
    private int maxThreadSize = 32;
    private int maxRetryTimes = 3;
    private String tableNameRegex = "^[a-zA-Z0-9_]+$";
}
