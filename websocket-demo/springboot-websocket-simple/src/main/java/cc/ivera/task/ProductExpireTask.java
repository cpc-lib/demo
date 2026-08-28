package cc.ivera.task;

import java.io.IOException;
import cc.ivera.push.ProductWebSocket;
import cc.ivera.util.TimeConversionUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * @Author：JCccc
 * @Description：
 * @Date： created in 15:56 2019/5/13
 */
@Component
public class ProductExpireTask {



    @Scheduled(fixedRate=2000)
    public void productExpire() throws IOException {
        String[] strs={"Test随机消息 ：30.1123",
                "Test随机消息 ：32.1021",
                "Test随机消息 ：33.1774",
                "Test随机消息 ：33.2372",
                "Test随机消息 ：31.0281",
                "Test随机消息 ：30.0222",
                "Test随机消息 ：32.1322",
                "Test随机消息 ：33.3221",
                "Test随机消息 ：31.2311",
                "Test随机消息 ：32.3112"};

        ProductWebSocket.sendInfo(TimeConversionUtil.getTimeStrNow()+"    Test 消息---->"+ RandomStr(strs));

    }

    //随机返回字符串数组中的字符串
    public static String RandomStr(String[] strs){
        int random_index = (int) (Math.random()*strs.length);
        return strs[random_index];
    }


}