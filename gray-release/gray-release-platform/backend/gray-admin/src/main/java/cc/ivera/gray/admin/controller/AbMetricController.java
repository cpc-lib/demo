package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.AbMetric;
import cc.ivera.gray.admin.service.AbMetricService;
import cc.ivera.gray.common.ApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ab-metrics")
public class AbMetricController {
    private final AbMetricService abMetricService;

    public AbMetricController(AbMetricService abMetricService) {
        this.abMetricService = abMetricService;
    }

    @GetMapping
    public ApiResponse<List<AbMetric>> list(@RequestParam(defaultValue = "demo-order-service") String serviceId,
                                            @RequestParam(defaultValue = "default-exp") String experimentKey) {
        return ApiResponse.ok(abMetricService.list(serviceId, experimentKey));
    }

    @PostMapping("/record")
    public ApiResponse<AbMetric> record(@RequestBody Map<String, Object> body) {
        String serviceId = String.valueOf(body.getOrDefault("serviceId", "demo-order-service"));
        String experimentKey = String.valueOf(body.getOrDefault("experimentKey", "default-exp"));
        String variant = String.valueOf(body.getOrDefault("variant", "A"));
        boolean converted = Boolean.parseBoolean(String.valueOf(body.getOrDefault("converted", "false")));
        return ApiResponse.ok(abMetricService.record(serviceId, experimentKey, variant, converted));
    }
}

