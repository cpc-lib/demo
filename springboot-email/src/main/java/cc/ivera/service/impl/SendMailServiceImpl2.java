package cc.ivera.service.impl;

import cc.ivera.service.SendMailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import javax.mail.internet.MimeMessage;
import java.io.File;

//@Service
//@Primary
public class SendMailServiceImpl2 implements SendMailService {

    @Autowired
    private JavaMailSender javaMailSender;

    //发送人
    private String from = "e2607439502@163.com";
    //接收人
    private String to = "2607439502@qq.com";
    //标题
    private String subject = "测试邮件";
    //正文
    private String context = "<img src='https://gimg2.baidu.com/image_search/src=http%3A%2F%2Fbkimg.cdn.bcebos.com%2Fpic%2F8326cffc1e178a82b9018131e84f648da97739124247&refer=http%3A%2F%2Fbkimg.cdn.bcebos.com&app=2002&size=f9999,10000&q=a80&n=0&g=0n&fmt=jpeg?sec=1645001879&t=f4d8895e53576eacf54605dcc63c6861'/><a href='https://www.itcast.cn'>点开有惊喜</a>";

    @Override
    public void sendMail() {

        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message,true);
            helper.setFrom(from+"(Hejiaming)");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(context,true);

            //添加附件
            //File f1 = new File("E:\\home\\wallhaven.zip");
            File f2 = new File("E:\\home\\1648370628234.jpg");

            //helper.addAttachment(f1.getName(),f1);
            helper.addAttachment("LOL.jpg",f2);

            javaMailSender.send(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

















