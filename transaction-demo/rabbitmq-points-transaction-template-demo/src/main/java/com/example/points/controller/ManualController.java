package com.example.points.controller;

import com.example.points.service.ManualProcessService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/manual")
@RequiredArgsConstructor
public class ManualController {

    private final ManualProcessService manualProcessService;

    // 人工处理后，点击按钮重新发送消息
    @PostMapping("/resend/{id}")
    public String resend(@PathVariable("id") Long id) {
        manualProcessService.reSend(id);
        return "ok";
    }
}
