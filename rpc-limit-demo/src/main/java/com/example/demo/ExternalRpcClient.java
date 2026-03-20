package com.example.demo;

import org.springframework.stereotype.Component;

@Component
public class ExternalRpcClient {
    public String callExternalApi() {
        return "external api ok";
    }
}
