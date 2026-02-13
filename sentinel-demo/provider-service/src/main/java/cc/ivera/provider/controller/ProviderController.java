package cc.ivera.provider.controller;

import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/provider")
public class ProviderController {

  @GetMapping("/hello")
  public Map<String, Object> hello(@RequestParam(defaultValue = "world") String name) {
    return Map.of("msg", "hello " + name, "from", "provider");
  }

  @GetMapping("/slow")
  public Map<String, Object> slow(@RequestParam(defaultValue = "800") long ms) throws InterruptedException {
    Thread.sleep(ms);
    return Map.of("msg", "slow ok", "sleepMs", ms, "from", "provider");
  }

  @GetMapping("/error")
  public Map<String, Object> error() {
    throw new RuntimeException("mock provider exception");
  }
}
