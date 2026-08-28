package cc.ivera.dtx.txmsgdemo.bank2.message;

import cc.ivera.dtx.txmsgdemo.bank2.model.AccountChangeEvent;
import cc.ivera.dtx.txmsgdemo.bank2.service.AccountInfoService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@RocketMQMessageListener(consumerGroup = "consumer_txmsg_group_bank2",topic = "topic_txmsg")
@Slf4j
public class TxmsgConsumer implements RocketMQListener<String> {

    @Autowired
    AccountInfoService accountInfoService;

    @Override
    public void onMessage(String message) {
        log.info("开始消费消息:{}", message);
        //解析消息为对象
        final JSONObject jsonObject = JSON.parseObject(message);
        AccountChangeEvent accountChangeEvent =
                JSONObject.parseObject(jsonObject.getString("accountChange"), AccountChangeEvent.class);
        //调用service增加账号金额（收款方+钱）
        //修改为李四的账号
        accountChangeEvent.setAccountNo("2");
        //加钱+写入消费表（事务消息）
        accountInfoService.addAccountInfoBalance(accountChangeEvent);
    }
}