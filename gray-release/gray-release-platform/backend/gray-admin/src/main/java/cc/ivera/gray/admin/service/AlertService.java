package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.AlertEvent;
import cc.ivera.gray.admin.mapper.AlertEventMapper;
import cc.ivera.gray.common.GrayEnums.AlertLevel;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AlertService {
    private final AlertEventMapper alertEventMapper;
    private final RestClient restClient;
    private final String webhookUrl;

    public AlertService(AlertEventMapper alertEventMapper,
                        @Value("${gray.alert.webhook-url:}") String webhookUrl) {
        this.alertEventMapper = alertEventMapper;
        this.restClient = RestClient.create();
        this.webhookUrl = webhookUrl;
    }

    public AlertEvent create(AlertLevel level, String source, String title, String content) {
        AlertEvent event = new AlertEvent();
        event.setLevel(level.name());
        event.setSource(source);
        event.setTitle(title);
        event.setContent(content);
        event.setHandled(false);
        alertEventMapper.insert(event);
        notifyWebhook(event);
        return event;
    }

    public List<AlertEvent> latest() {
        return alertEventMapper.selectList(new LambdaQueryWrapper<AlertEvent>()
                .orderByDesc(AlertEvent::getCreateTime)
                .last("limit 20"));
    }

    private void notifyWebhook(AlertEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .body(Map.of(
                            "level", event.getLevel(),
                            "source", event.getSource(),
                            "title", event.getTitle(),
                            "content", event.getContent()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception ignored) {
            // 告警外发失败不影响发布主链路，事件已经落库。
        }
    }
}
