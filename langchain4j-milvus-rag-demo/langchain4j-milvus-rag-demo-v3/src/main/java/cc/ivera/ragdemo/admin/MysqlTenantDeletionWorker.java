package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.TenantDeletionSqlMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class MysqlTenantDeletionWorker implements TenantDeletionWorker {

    private static final Pattern TABLE_NAME_PATTERN = Pattern.compile("[A-Za-z0-9_]+");

    private final RagProperties properties;
    private final TenantDeletionSqlMapper deletionSqlMapper;

    @Override
    public String stageCode() {
        return TenantDeletionStageCode.MYSQL.name();
    }

    @Override
    public TenantDeletionStageResult dryRun(TenantDataDeletionTask task) {
        return TenantDeletionStageResult.success(stageCode(), countRows(task), detail(countRowsByTable(task)));
    }

    @Override
    public TenantDeletionStageResult execute(TenantDataDeletionTask task) {
        if (!properties.getTenantDeletion().isMysqlDeleteEnabled()) {
            return TenantDeletionStageResult.skipped(stageCode(), "mysql-delete-disabled");
        }
        long deleted = 0;
        for (String table : properties.getTenantDeletion().getMysqlTenantScopedTables()) {
            deleted += deletionSqlMapper.deleteTenantRows(safeTable(table), task.getTenantId());
        }
        return TenantDeletionStageResult.success(stageCode(), deleted, detail(countRowsByTable(task)));
    }

    @Override
    public TenantDeletionStageResult verify(TenantDataDeletionTask task) {
        long remaining = countRows(task);
        if (remaining == 0 || !properties.getTenantDeletion().isMysqlDeleteEnabled()) {
            return TenantDeletionStageResult.success(stageCode(), remaining, detail(countRowsByTable(task)));
        }
        return TenantDeletionStageResult.failed(stageCode(), "MYSQL_TENANT_ROWS_REMAIN", "Remaining tenant rows: " + remaining);
    }

    private long countRows(TenantDataDeletionTask task) {
        return countRowsByTable(task).values().stream().mapToLong(Long::longValue).sum();
    }

    private Map<String, Long> countRowsByTable(TenantDataDeletionTask task) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (String table : properties.getTenantDeletion().getMysqlTenantScopedTables()) {
            try {
                counts.put(table, deletionSqlMapper.countTenantRows(safeTable(table), task.getTenantId()));
            } catch (Exception ex) {
                counts.put(table, -1L);
            }
        }
        return counts;
    }

    private String safeTable(String table) {
        if (table == null || !TABLE_NAME_PATTERN.matcher(table).matches()) {
            throw new IllegalArgumentException("Invalid tenant scoped table name: " + table);
        }
        return table;
    }

    private String detail(Map<String, Long> counts) {
        StringBuilder json = new StringBuilder("{\"tables\":{");
        int i = 0;
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            if (i++ > 0) {
                json.append(',');
            }
            json.append('"').append(entry.getKey()).append("\":").append(entry.getValue());
        }
        json.append("}}");
        return json.toString();
    }
}
