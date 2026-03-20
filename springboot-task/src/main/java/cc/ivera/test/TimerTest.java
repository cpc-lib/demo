package cc.ivera.test;

import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import cc.ivera.Application;
import cc.ivera.util.DateUtils;

import java.util.Timer;
import java.util.TimerTask;

@SpringBootTest(classes={Application.class})
public class TimerTest {

//    public static void main(String[] args) {
//        Timer timer = new Timer();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                System.out.println("timer task run..." + DateUtils.getTime());
//            }
//        };
//        timer.schedule(task,0,2000);
//    }

    @Test
    public void testTimer(){
        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                System.out.println("timer task run..." + DateUtils.getTime());
            }
        };
        timer.schedule(task,0,2000);
    }
}
