package cc.ivera.migration.domain;

import lombok.Data;

@Data
public class PlanRequest {
    private String sourceTable = "user_source";
    private String targetTable = "user_target";
    private String idColumn = "id";
    private Integer shardSize;
    private Integer batchSize;
    private Integer threadSize;
    private Boolean useTempTable = true;
    private Boolean clearTargetBeforeSwitch = true;
}
