package cc.ivera.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * @Author : JCccc
 * @CreateTime : 2019/10/8
 * @Description :
 **/

@Controller
public class WebSocketController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @RequestMapping("/websocket/{name}")
    public String webSocket(@PathVariable String name, Model model) {
        try {
            logger.info("跳转到websocket的页面上");
            System.out.println(name);
            model.addAttribute("username", name);
            return "socketA";
        } catch (Exception e) {
            logger.info("跳转到websocket的页面上发生异常，异常信息是：" + e.getMessage());
            return "error";
        }
    }

    @RequestMapping("/websocketS/{name}")
    public String websocketS(@PathVariable String name, Model model) {
        try {
            logger.info("跳转到websocket的页面上");
            System.out.println(name);
            model.addAttribute("username", name);
            return "socketB";
        } catch (Exception e) {
            logger.info("跳转到websocket的页面上发生异常，异常信息是：" + e.getMessage());
            return "error";
        }
    }

    @RequestMapping("/testPage")
    public String testPage() {
        return "testPage";
    }
}