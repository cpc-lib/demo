package cc.ivera.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.common.RemotingHelper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

/**
 * 微信公众号：三友的java日记
 *
 * @author sanyou
 * @date 2023/2/26 21:11
 */
@RestController
@Slf4j
public class RocketmqDelayTaskController {

    @Resource
    private DefaultMQProducer producer;

    @GetMapping("/rocketmq/add")
    public void addTask(@RequestParam("task") String task) throws Exception {
        Message msg = new Message("sanyouDelayTaskTopic", "TagA", task.getBytes(RemotingHelper.DEFAULT_CHARSET));
        msg.setDelayTimeLevel(2);
        // 发送消息并得到消息的发送结果，然后打印
        log.info("提交延迟任务");
        producer.send(msg);
    }

}
