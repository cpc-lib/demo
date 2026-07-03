package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.ServicePolicy;
import cc.ivera.gray.admin.service.ServicePolicyService;
import cc.ivera.gray.common.ApiResponse;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/policies")
public class ServicePolicyController {
    private final ServicePolicyService servicePolicyService;

    public ServicePolicyController(ServicePolicyService servicePolicyService) {
        this.servicePolicyService = servicePolicyService;
    }

    @GetMapping
    public ApiResponse<ServicePolicy> get(@RequestParam String serviceId) {
        return ApiResponse.ok(servicePolicyService.getOrCreate(serviceId));
    }

    @PostMapping("/blue-green/switch")
    public ApiResponse<ServicePolicy> switchBlueGreen(@RequestBody Map<String, String> body,
                                                      @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(servicePolicyService.blueGreenSwitch(
                body.getOrDefault("serviceId", "demo-order-service"),
                body.getOrDefault("activeColor", "blue"),
                operator));
    }

    @PostMapping("/ab")
    public ApiResponse<ServicePolicy> configureAb(@RequestBody Map<String, Object> body,
                                                  @RequestHeader(value = "X-Operator", required = false) String operator) {
        String serviceId = String.valueOf(body.getOrDefault("serviceId", "demo-order-service"));
        boolean enabled = Boolean.parseBoolean(String.valueOf(body.getOrDefault("enabled", "false")));
        int percentB = Integer.parseInt(String.valueOf(body.getOrDefault("percentB", "50")));
        return ApiResponse.ok(servicePolicyService.configureAb(serviceId, enabled, percentB, operator));
    }
}

