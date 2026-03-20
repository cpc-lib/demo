package top.arhi.wxpush;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.InetAddress;

@Order(1)
@Component
public class StartLog implements CommandLineRunner {

    @Resource
    private Environment environment;

    @Override
    public void run(String... args) throws Exception {
        StringBuilder commandLog = new StringBuilder();
        String ip = InetAddress.getLocalHost().getHostAddress();
        String port = environment.getProperty("server.port");
        commandLog.append("+=========================================================================================+\n");
        commandLog.append("+                         WeChat push started successfully!!                              +\n");
        commandLog.append("+    You can open url(http://" + ip + ":" + port + "/send) to send WeChat message!                   +\n");
        commandLog.append("+    Of course,you also can open url(http://" + ip + ":" + port + "/) to change weather of city      +\n");
        System.out.println(commandLog.toString());
    }
}

