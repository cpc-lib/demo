package cc.ivera.enums.reconciliation;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum BatchStatus {

    CREATED("已创建"),

    BILL_DOWNLOADED("账单已下载"),

    LOCAL_COLLECTED("本地数据已采集"),

    MATCHED("已对账"),

    DISCREPANCY_PENDING("差异待处理"),

    RESOLVED("差异已处理"),

    COMPLETED("已完成"),

    FAILED("失败");

    private final String type;
}
