package cc.ivera.gray.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cc.ivera.gray.admin.entity.ReleaseTask;
import cc.ivera.gray.admin.mapper.ReleaseTaskMapper;
import cc.ivera.gray.admin.service.AlertService;
import cc.ivera.gray.admin.service.AuditService;
import cc.ivera.gray.admin.service.ReleaseApprovalService;
import cc.ivera.gray.admin.service.ReleaseTaskService;
import cc.ivera.gray.admin.service.ServicePolicyService;
import cc.ivera.gray.common.GrayEnums.AlertLevel;
import cc.ivera.gray.common.GrayEnums.ReleaseStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class ReleaseTaskServiceTest {
    @Test
    void shouldAutoRollbackWhenMetricsExceedThreshold() {
        ReleaseTaskMapper mapper = mock(ReleaseTaskMapper.class);
        AuditService auditService = mock(AuditService.class);
        ReleaseApprovalService approvalService = mock(ReleaseApprovalService.class);
        AlertService alertService = mock(AlertService.class);
        ServicePolicyService policyService = mock(ServicePolicyService.class);
        ReleaseTaskService service = new ReleaseTaskService(
                mapper,
                auditService,
                approvalService,
                alertService,
                policyService,
                new ObjectMapper());

        ReleaseTask task = new ReleaseTask();
        task.setId(1L);
        task.setTaskName("demo canary");
        task.setStatus(ReleaseStatus.RUNNING.name());
        task.setCurrentPercent(20);
        task.setAutoRollbackEnabled(true);
        task.setErrorRateThreshold(0.05);
        task.setP99LatencyThresholdMs(1200);
        when(mapper.selectById(1L)).thenReturn(task);

        ReleaseTask result = service.reportMetrics(1L, 0.12, 1800, "test");

        assertEquals(ReleaseStatus.ROLLED_BACK.name(), result.getStatus());
        assertEquals(0, result.getCurrentPercent());
        verify(mapper).updateById(task);
        verify(alertService).create(eq(AlertLevel.CRITICAL), eq("release-task-1"), eq("发布任务自动回滚"), any());
    }
}

