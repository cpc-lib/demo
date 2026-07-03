package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.ReleaseApproval;
import cc.ivera.gray.admin.security.SecurityUtil;
import cc.ivera.gray.admin.service.ReleaseApprovalService;
import cc.ivera.gray.common.ApiResponse;
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
@RequestMapping("/api/approvals")
public class ReleaseApprovalController {
    private final ReleaseApprovalService releaseApprovalService;

    public ReleaseApprovalController(ReleaseApprovalService releaseApprovalService) {
        this.releaseApprovalService = releaseApprovalService;
    }

    @GetMapping
    public ApiResponse<List<ReleaseApproval>> list(@RequestParam(required = false) Long taskId) {
        return ApiResponse.ok(releaseApprovalService.list(taskId));
    }

    @PostMapping("/{id}/approve")
    public ApiResponse<ReleaseApproval> approve(@PathVariable Long id,
                                                @RequestBody(required = false) Map<String, String> body,
                                                @RequestHeader(value = "X-Operator", required = false) String operator) {
        String comment = body == null ? "审批通过" : body.getOrDefault("comment", "审批通过");
        return ApiResponse.ok(releaseApprovalService.approve(id, SecurityUtil.currentUser(operator), comment));
    }

    @PostMapping("/{id}/reject")
    public ApiResponse<ReleaseApproval> reject(@PathVariable Long id,
                                               @RequestBody(required = false) Map<String, String> body,
                                               @RequestHeader(value = "X-Operator", required = false) String operator) {
        String comment = body == null ? "审批拒绝" : body.getOrDefault("comment", "审批拒绝");
        return ApiResponse.ok(releaseApprovalService.reject(id, SecurityUtil.currentUser(operator), comment));
    }
}

