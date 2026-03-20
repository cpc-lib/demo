package cc.ivera.controller;

import cc.ivera.config.ConfigA;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@ResponseBody
@RequestMapping("/")
public class Test {

    @Autowired
    private ConfigA client;

    @GetMapping("/test")
    public void  test(){
        //单例模式,地址相同。
        System.out.println(Thread.currentThread().getName()+":"+client.elasticClient());
    }


    @GetMapping("/hello")
    public String hello(){
     return "<h1 style=color:red>Hello,welcome to America!</h1>";
    }
}
