package cc.ivera.mq;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.Date;


@RequestMapping("/testMq")
@RestController
public class ActivemqDelayTaskController {
    private final ActivemqOperator activemqOperator;

    //构造方式注入bean
    @Autowired
    public ActivemqDelayTaskController(ActivemqOperator activemqOperator) {
        this.activemqOperator = activemqOperator;
    }

    @GetMapping("/delayqueue")
    public String testDelayQueue() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.println("当前订单【123456789】生成时间： " + df.format(new Date()));// new Date()为获取当前系统时间
        //设置延时时间推送到activeMQ
        activemqOperator.delaySend("delayqueue", "延时10S", 10000L);
        return "delay";
    }


    @GetMapping("/normalqueue")
    public String testNormalQueue() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.println("当前订单【123456789】生成时间： " + df.format(new Date()));// new Date()为获取当前系统时间
        //设置延时时间推送到activeMQ
        activemqOperator.sendMsgQueue("normalqueue", "Hello world!");
        return "normal";
    }

    @JmsListener(destination = "delayqueue", containerFactory = "queueJmsListenerContainerFactory")
    public void delayQueueListener(String message) {
        //业务逻辑,根据单号查询是否已经付款
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.println("当前订单【123456789】失效时间： " + df.format(new Date()));// new Date()为获取当前系统时间
        System.out.println(message);
    }

    @JmsListener(destination = "normalqueue", containerFactory = "queueJmsListenerContainerFactory")
    public void normalQueueListener(String message) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
        System.out.println("当前订单【123456789】失效时间： " + df.format(new Date()));// new Date()为获取当前系统时间
        System.out.println(message);
    }
}
