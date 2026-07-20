package cc.ivera.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ReconciliationStatus {

    PENDING("待执行"),

    PROCESSING("执行中"),

    COMPLETED("已完成"),

    FAILED("执行失败");

    private final String description;
}
