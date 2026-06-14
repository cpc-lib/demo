package cc.ivera.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ChangeLogResponse {
    private Long logId;
    private String bizType;
    private String bizId;
    private String operationType;
    private String logMode;
    private String operator;
    private Integer changedFieldCount;
    private String remark;
    private LocalDateTime createdAt;
    private List<ChangeLogDetailItem> details;

    @Data
    public static class ChangeLogDetailItem {
        private String fieldName;
        private String fieldLabel;
        private String oldValue;
        private String newValue;
        private Integer changed;
    }
}
