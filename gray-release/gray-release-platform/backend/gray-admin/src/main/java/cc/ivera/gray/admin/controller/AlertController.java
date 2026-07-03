package cc.ivera.gray.admin.controller;

import cc.ivera.gray.admin.entity.AlertEvent;
import cc.ivera.gray.admin.service.AlertService;
import cc.ivera.gray.common.ApiResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {
    private final AlertService alertService;

    public AlertController(AlertService alertService) {
        this.alertService = alertService;
    }

    @GetMapping
    public ApiResponse<List<AlertEvent>> latest() {
        return ApiResponse.ok(alertService.latest());
    }
}

