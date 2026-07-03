package cc.ivera.gray.admin.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.AuditLog;
import cc.ivera.gray.admin.entity.GrayRule;
import cc.ivera.gray.admin.entity.ReleaseTask;
import cc.ivera.gray.admin.mapper.AlertEventMapper;
import cc.ivera.gray.admin.mapper.AuditLogMapper;
import cc.ivera.gray.admin.mapper.GrayRuleMapper;
import cc.ivera.gray.admin.mapper.ReleaseTaskMapper;
import cc.ivera.gray.common.ApiResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final GrayRuleMapper grayRuleMapper;
    private final ReleaseTaskMapper releaseTaskMapper;
    private final AuditLogMapper auditLogMapper;
    private final AlertEventMapper alertEventMapper;

    public DashboardController(GrayRuleMapper grayRuleMapper,
                               ReleaseTaskMapper releaseTaskMapper,
                               AuditLogMapper auditLogMapper,
                               AlertEventMapper alertEventMapper) {
        this.grayRuleMapper = grayRuleMapper;
        this.releaseTaskMapper = releaseTaskMapper;
        this.auditLogMapper = auditLogMapper;
        this.alertEventMapper = alertEventMapper;
    }

    @GetMapping("/summary")
    public ApiResponse<Map<String, Object>> summary() {
        long totalRules = grayRuleMapper.selectCount(null);
        long enabledRules = grayRuleMapper.selectCount(new LambdaQueryWrapper<GrayRule>().eq(GrayRule::getEnabled, true));
        long runningTasks = releaseTaskMapper.selectCount(new LambdaQueryWrapper<ReleaseTask>().eq(ReleaseTask::getStatus, "RUNNING"));
        List<AuditLog> latestAudits = auditLogMapper.selectList(new LambdaQueryWrapper<AuditLog>()
                .orderByDesc(AuditLog::getCreateTime)
                .last("limit 8"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalRules", totalRules);
        data.put("enabledRules", enabledRules);
        data.put("runningTasks", runningTasks);
        data.put("criticalAlerts", alertEventMapper.selectCount(null));
        data.put("latestAudits", latestAudits);
        data.put("services", List.of("demo-order-service"));
        return ApiResponse.ok(data);
    }
}
