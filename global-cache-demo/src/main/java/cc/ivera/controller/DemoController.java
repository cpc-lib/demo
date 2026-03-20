package cc.ivera.controller;

import cc.ivera.common.annotation.Cache;
import cc.ivera.common.bean.ResponseBean;
import cc.ivera.common.bean.User;
import cc.ivera.service.DemoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class DemoController {
    @Autowired
    private DemoService demoService;

    @GetMapping("action1")
    public ResponseBean action1() {
        Map<String,Object> result = this.demoService.action1();
        return ResponseBean.ok(result);
    }

    //@Cache(time = "35",expression = "#user.getUserName(),#sex")
    @Cache(time = "35",key = "myUser",expression = "#user.getUserName(),#sex")
    @GetMapping("action2")
    public ResponseBean action2(User user, String sex) {
        Map<String,Object> result = this.demoService.action2();
        return ResponseBean.ok(result);
    }

    @RequestMapping("action3")
    public ResponseBean action3() {
        Map<String,Object> result = this.demoService.action3();
        return ResponseBean.ok(result);
    }

    @RequestMapping("action4")
    public ResponseBean action4() {
        Map<String,Object> result = this.demoService.action4();
        return ResponseBean.ok(result);
    }

}