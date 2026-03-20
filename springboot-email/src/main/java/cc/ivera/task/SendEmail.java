package cc.ivera.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cc.ivera.service.SendMailService;

@Component
public class SendEmail {

    @Autowired
    private SendMailService sendMailService;

    @Scheduled(cron="0 35 18 * * ? ")
    public void execute(){
        sendMailService.sendMail();
    }

}
