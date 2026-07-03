package cc.ivera.gray.demo.v2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/order")
public class OrderController {
    @GetMapping("/health")
    public Map<String, Object> health(@RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        return Map.of(
                "service", "demo-order-service",
                "version", "v2",
                "status", "canary",
                "feature", "new-risk-check",
                "traceId", traceId == null ? "" : traceId,
                "time", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/orders")
    public Map<String, Object> orders() {
        return Map.of(
                "version", "v2",
                "items", List.of(
                        Map.of("orderNo", "B20001", "amount", 199.00, "status", "PAID", "riskLevel", "LOW"),
                        Map.of("orderNo", "B20002", "amount", 59.90, "status", "CREATED", "riskLevel", "LOW")
                )
        );
    }
}
