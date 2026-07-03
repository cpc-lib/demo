package cc.ivera.gray.demo.v1;

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
                "version", "v1",
                "status", "stable",
                "traceId", traceId == null ? "" : traceId,
                "time", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/orders")
    public Map<String, Object> orders() {
        return Map.of(
                "version", "v1",
                "items", List.of(
                        Map.of("orderNo", "A10001", "amount", 199.00, "status", "PAID"),
                        Map.of("orderNo", "A10002", "amount", 59.90, "status", "CREATED")
                )
        );
    }
}
