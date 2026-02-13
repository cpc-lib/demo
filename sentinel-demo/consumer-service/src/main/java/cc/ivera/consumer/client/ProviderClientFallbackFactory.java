package cc.ivera.consumer.client;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ProviderClientFallbackFactory implements FallbackFactory<ProviderClient> {

  @Override
  public ProviderClient create(Throwable cause) {
    return new ProviderClient() {
      @Override
      public Map<String, Object> hello(String name) {
        return Map.of("msg", "feign fallback: hello", "name", name, "reason", String.valueOf(cause));
      }

      @Override
      public Map<String, Object> slow(long ms) {
        return Map.of("msg", "feign fallback: slow", "sleepMs", ms, "reason", String.valueOf(cause));
      }

      @Override
      public Map<String, Object> error() {
        return Map.of("msg", "feign fallback: error", "reason", String.valueOf(cause));
      }
    };
  }
}
