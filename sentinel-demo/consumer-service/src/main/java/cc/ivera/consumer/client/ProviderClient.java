package cc.ivera.consumer.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@FeignClient(
    name = "provider-service",
    path = "/api/provider",
    fallbackFactory = ProviderClientFallbackFactory.class
)
public interface ProviderClient {

  @GetMapping("/hello")
  Map<String, Object> hello(@RequestParam("name") String name);

  @GetMapping("/slow")
  Map<String, Object> slow(@RequestParam("ms") long ms);

  @GetMapping("/error")
  Map<String, Object> error();
}
