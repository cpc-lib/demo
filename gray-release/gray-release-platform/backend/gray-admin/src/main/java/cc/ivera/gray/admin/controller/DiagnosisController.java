package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.service.GrayRuleService;
import cc.ivera.gray.common.ApiResponse;
import cc.ivera.gray.common.GrayMatchRequest;
import cc.ivera.gray.common.GrayMatchResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {
    private final GrayRuleService grayRuleService;

    public DiagnosisController(GrayRuleService grayRuleService) {
        this.grayRuleService = grayRuleService;
    }

    @PostMapping("/match")
    public ApiResponse<GrayMatchResult> match(@Valid @RequestBody GrayMatchRequest request) {
        return ApiResponse.ok(grayRuleService.match(request));
    }
}

