package com.example.dubbolimit;

import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class EchoServiceImpl implements EchoService {

    @DubboLimit(value = 5, key = "echo")
    @Override
    public String echo(String msg) {
        return "Echo: " + msg;
    }
}
