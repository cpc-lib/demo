package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.GrayRule;
import cc.ivera.gray.admin.service.GrayRuleService;
import cc.ivera.gray.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rules")
public class GrayRuleController {
    private final GrayRuleService grayRuleService;

    public GrayRuleController(GrayRuleService grayRuleService) {
        this.grayRuleService = grayRuleService;
    }

    @GetMapping
    public ApiResponse<List<GrayRule>> list(@RequestParam(required = false) String serviceId) {
        return ApiResponse.ok(grayRuleService.list(serviceId));
    }

    @PostMapping
    public ApiResponse<GrayRule> create(@RequestBody GrayRule rule,
                                        @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(grayRuleService.create(rule, operator));
    }

    @PostMapping("/conflicts")
    public ApiResponse<List<GrayRule>> conflicts(@RequestBody GrayRule rule) {
        return ApiResponse.ok(grayRuleService.findConflicts(rule, rule.getId()));
    }

    @PostMapping("/publish")
    public ApiResponse<Boolean> publish(@RequestParam String serviceId) {
        return ApiResponse.ok(grayRuleService.publishRules(serviceId));
    }

    @PutMapping("/{id}")
    public ApiResponse<GrayRule> update(@PathVariable Long id,
                                        @RequestBody GrayRule rule,
                                        @RequestHeader(value = "X-Operator", required = false) String operator) {
        return ApiResponse.ok(grayRuleService.update(id, rule, operator));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id,
                                    @RequestHeader(value = "X-Operator", required = false) String operator) {
        grayRuleService.delete(id, operator);
        return ApiResponse.ok(null);
    }
}
