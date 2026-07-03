package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.ReleaseTask;
import cc.ivera.gray.admin.service.ReleaseTaskService;
import cc.ivera.gray.common.ApiResponse;
import cc.ivera.gray.common.GrayEnums.ReleaseStatus;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/releases")
public class ReleaseTaskController {
    private final ReleaseTaskService releaseTaskService;

    public ReleaseTaskController(ReleaseTaskService releaseTaskService) {
        this.releaseTaskService = releaseTaskService;
    }

    @GetMapping
    public ApiResponse<List<ReleaseTask>> list(@RequestParam(required = false) String serviceId) {
        return ApiResponse.ok(releaseTaskService.list(serviceId));
    }

    @PostMapping
    public ApiResponse<ReleaseTask> create(@RequestBody ReleaseTask task,
                                           @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(releaseTaskService.create(task, operator));
    }

    @PostMapping("/{id}/start")
    public ApiResponse<ReleaseTask> start(@PathVariable Long id,
                                          @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(releaseTaskService.changeStatus(id, ReleaseStatus.RUNNING, null, operator));
    }

    @PostMapping("/{id}/pause")
    public ApiResponse<ReleaseTask> pause(@PathVariable Long id,
                                          @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(releaseTaskService.changeStatus(id, ReleaseStatus.PAUSED, null, operator));
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<ReleaseTask> complete(@PathVariable Long id,
                                             @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(releaseTaskService.changeStatus(id, ReleaseStatus.COMPLETED, null, operator));
    }

    @PostMapping("/{id}/rollback")
    public ApiResponse<ReleaseTask> rollback(@PathVariable Long id,
                                             @RequestBody(required = false) Map<String, String> body,
                                             @RequestHeader(value = "X-Operator", required = false) String operator) {
        String reason = body == null ? "manual rollback" : body.getOrDefault("reason", "manual rollback");
        return ApiResponse.ok(releaseTaskService.changeStatus(id, ReleaseStatus.ROLLED_BACK, reason, operator));
    }

    @PostMapping("/{id}/advance")
    public ApiResponse<ReleaseTask> advance(@PathVariable Long id,
                                            @RequestBody Map<String, Integer> body,
                                            @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(releaseTaskService.advance(id, body.getOrDefault("percent", 0), operator));
    }

    @PostMapping("/{id}/metrics")
    public ApiResponse<ReleaseTask> metrics(@PathVariable Long id,
                                            @RequestBody Map<String, Number> body,
                                            @RequestHeader(value = "X-Operator", required = false) String operator) {
        Double errorRate = body.get("errorRate") == null ? null : body.get("errorRate").doubleValue();
        Integer p99 = body.get("p99LatencyMs") == null ? null : body.get("p99LatencyMs").intValue();
        return ApiResponse.ok(releaseTaskService.reportMetrics(id, errorRate, p99, operator));
    }
}
