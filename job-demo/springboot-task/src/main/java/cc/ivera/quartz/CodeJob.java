package cc.ivera.quartz;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import cc.ivera.util.DateUtils;
/**
 * spring-task
 * */
@Component
public class CodeJob {

    @Scheduled(cron = "0/1 * * * * ?")
    public void execute() {

        System.out.println("spring task is running... " + DateUtils.getTime());
    }

}
