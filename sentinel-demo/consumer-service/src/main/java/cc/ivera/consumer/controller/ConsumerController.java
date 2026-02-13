package cc.ivera.consumer.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import cc.ivera.consumer.client.ProviderClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/consumer")
public class ConsumerController {

  private final ProviderClient providerClient;

  public ConsumerController(ProviderClient providerClient) {
    this.providerClient = providerClient;
  }

  @GetMapping("/callHello")
  @SentinelResource(value = "consumer_callHello", blockHandler = "blockCallHello", fallback = "fallbackCallHello")
  public Map<String, Object> callHello(@RequestParam(defaultValue = "jim") String name) {
    return providerClient.hello(name);
  }

  @GetMapping("/callSlow")
  @SentinelResource(value = "consumer_callSlow", blockHandler = "blockCallSlow", fallback = "fallbackCallSlow")
  public Map<String, Object> callSlow(@RequestParam(defaultValue = "800") long ms) {
    return providerClient.slow(ms);
  }

  @GetMapping("/callError")
  @SentinelResource(value = "consumer_callError", blockHandler = "blockCallError", fallback = "fallbackCallError")
  public Map<String, Object> callError() {
    return providerClient.error();
  }

  // Sentinel 规则触发（流控/降级/系统保护等）会走 blockHandler
  public Map<String, Object> blockCallHello(String name, BlockException ex) {
    return Map.of("msg", "blocked by sentinel", "resource", "consumer_callHello", "rule", ex.getClass().getSimpleName());
  }

  public Map<String, Object> blockCallSlow(long ms, BlockException ex) {
    return Map.of("msg", "blocked by sentinel", "resource", "consumer_callSlow", "rule", ex.getClass().getSimpleName());
  }

  public Map<String, Object> blockCallError(BlockException ex) {
    return Map.of("msg", "blocked by sentinel", "resource", "consumer_callError", "rule", ex.getClass().getSimpleName());
  }

  // 业务异常兜底（fallback）
  public Map<String, Object> fallbackCallHello(String name, Throwable t) {
    return Map.of("msg", "fallback", "resource", "consumer_callHello", "reason", String.valueOf(t));
  }

  public Map<String, Object> fallbackCallSlow(long ms, Throwable t) {
    return Map.of("msg", "fallback", "resource", "consumer_callSlow", "reason", String.valueOf(t));
  }

  public Map<String, Object> fallbackCallError(Throwable t) {
    return Map.of("msg", "fallback", "resource", "consumer_callError", "reason", String.valueOf(t));
  }
}
