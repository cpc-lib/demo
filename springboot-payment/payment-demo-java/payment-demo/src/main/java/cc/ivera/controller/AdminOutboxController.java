package cc.ivera.controller;

import cc.ivera.security.AuthContext;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@Api(tags = "管理员消息重投")
@RestController
@RequestMapping("/api/admin/outbox")
@Validated
public class AdminOutboxController {

    private final MessageOutboxService messageOutboxService;

    public AdminOutboxController(MessageOutboxService messageOutboxService) {
        this.messageOutboxService = messageOutboxService;
    }

    @ApiOperation("重投失败消息")
    @PostMapping("/{eventId}/retry")
    public R<Map<String, Object>> retry(
            @PathVariable
            @NotBlank(message = "消息事件ID不能为空")
            @Size(max = 36, message = "消息事件ID长度不能超过36") String eventId
    ) {
        AuthContext.requireAdmin();
        messageOutboxService.retryFailed(eventId);
        return R.ok().setMessage("消息已重新进入待投递状态");
    }
}
