package cc.ivera.migration.controller;

import cc.ivera.migration.domain.ApiResult;
import cc.ivera.migration.domain.PlanRequest;
import cc.ivera.migration.service.MigrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/migration")
@RequiredArgsConstructor
public class MigrationController {
    private final MigrationService migrationService;

    @PostMapping("/plan")
    public ApiResult<Map<String, Object>> plan(@RequestBody PlanRequest request) {
        return ApiResult.ok(migrationService.plan(request));
    }

    @PostMapping("/execute/{taskNo}")
    public ApiResult<Map<String, Object>> execute(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.execute(taskNo));
    }

    @PostMapping("/retry-failed/{taskNo}")
    public ApiResult<Map<String, Object>> retryFailed(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.retryFailed(taskNo));
    }

    @PostMapping("/verify/{taskNo}")
    public ApiResult<Map<String, Object>> verify(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.verify(taskNo));
    }

    @PostMapping("/switch/{taskNo}")
    public ApiResult<Map<String, Object>> switchTable(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.switchTable(taskNo));
    }

    @PostMapping("/plan-execute-verify-switch")
    public ApiResult<Map<String, Object>> full(@RequestBody PlanRequest request) {
        Map<String, Object> plan = migrationService.plan(request);
        String taskNo = String.valueOf(plan.get("taskNo"));
        migrationService.execute(taskNo);
        migrationService.retryFailed(taskNo);
        migrationService.verify(taskNo);
        return ApiResult.ok(migrationService.switchTable(taskNo));
    }

    @GetMapping("/task/{taskNo}")
    public ApiResult<Map<String, Object>> task(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.task(taskNo));
    }

    @GetMapping("/shards/{taskNo}")
    public ApiResult<Object> shards(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.shards(taskNo));
    }

    @GetMapping("/batch-logs/{taskNo}")
    public ApiResult<Object> batchLogs(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.batchLogs(taskNo));
    }

    @GetMapping("/logs/{taskNo}")
    public ApiResult<Object> logs(@PathVariable("taskNo") String taskNo) {
        return ApiResult.ok(migrationService.logs(taskNo));
    }
}
