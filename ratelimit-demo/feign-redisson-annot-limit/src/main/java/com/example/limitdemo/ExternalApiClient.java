package com.example.limitdemo;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "externalApi",
        url = "${external.api.base-url:http://localhost:8080}"
)
public interface ExternalApiClient {

    @GetMapping("/get")
    String echo(@RequestParam(name = "from", required = false) String from);
}
