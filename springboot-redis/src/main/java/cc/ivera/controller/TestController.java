package cc.ivera.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import cc.ivera.service.DelayQueueService;


@Controller
public class TestController {

    @Autowired
    private DelayQueueService delayQueueService;

    @ResponseBody
    @GetMapping("/delayqueue")
    public String delayqueue() {
        for (int i = 0; i < 26; i++) {
            delayQueueService.addToDelayQueue("Hello world!", 5000L);
        }
        return "delayqueue";
    }
}
