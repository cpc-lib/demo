package com.example.dubbolimit;

import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class TestController {

    //@DubboReference(url = "dubbo://127.0.0.1:20880")
    @DubboReference
    private EchoService echoService;

    @GetMapping("/echo")
    public String test(@RequestParam(defaultValue = "hello") String msg) {
        return echoService.echo(msg);
    }

    @ExceptionHandler(DubboLimitException.class)
    public String handle(DubboLimitException e) {
        return e.getMessage();
    }
}
