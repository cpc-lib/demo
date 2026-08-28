package com.example.limitdemo;

import org.springframework.stereotype.Service;

@Service
public class ExternalBusinessService {

    private final ExternalApiClient externalApiClient;

    public ExternalBusinessService(ExternalApiClient externalApiClient) {
        this.externalApiClient = externalApiClient;
    }

    @ConcurrencyLimit(
            value = 5,
            key = "external-api",
            blocking = false,
            message = "外部系统并发数已满，请稍后再试"
    )
    public String callExternal() {
        return externalApiClient.echo("feign-redisson-demo");
    }

    @ConcurrencyLimit(
            value = 5,
            key = "local-slow-business",
            blocking = true,
            timeoutMs = 5000,
            message = "本地排队超时，请稍后再试"
    )
    public String localSlowBusiness() {
        try {
            Thread.sleep(2000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return "local slow business ok";
    }
}
