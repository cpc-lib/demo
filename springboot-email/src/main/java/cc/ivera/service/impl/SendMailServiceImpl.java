package cc.ivera.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import cc.ivera.service.SendMailService;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@Service
@Primary
public class SendMailServiceImpl implements SendMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    //发送人
    private String from = "e2607439502@163.com";
    //接收人
    private String to = "2607439502@qq.com";
    //标题
    private String subject = "software";
    //正文
    private String context = "https://pan.baidu.com/s/1oXbA24D9d-gbSyHWyeiWcQ&pwd=sz79";



    @Override
    public void sendMail() {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from+"(unknown)");
        message.setTo(to);
        message.setSubject(subject);
        message.setText(context);
        javaMailSender.send(message);
        System.out.println("发送成功!");
    }
}

















