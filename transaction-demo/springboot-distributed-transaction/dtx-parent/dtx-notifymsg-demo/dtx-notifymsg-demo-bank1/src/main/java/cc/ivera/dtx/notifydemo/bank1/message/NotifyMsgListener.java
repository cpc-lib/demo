package cc.ivera.dtx.notifydemo.bank1.message;

import cc.ivera.dtx.notifydemo.bank1.entity.AccountPay;
import cc.ivera.dtx.notifydemo.bank1.model.AccountChangeEvent;
import cc.ivera.dtx.notifydemo.bank1.service.AccountInfoService;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RocketMQMessageListener(topic = "topic_notifymsg", consumerGroup = "consumer_group_notifymsg_bank1")
public class NotifyMsgListener implements RocketMQListener<AccountPay> {

    @Autowired
    AccountInfoService accountInfoService;

    @Override
    public void onMessage(AccountPay accountPay) {
        log.info("接收到消息：{}", JSON.toJSONString(accountPay));
        AccountChangeEvent accountChangeEvent = new AccountChangeEvent();
        accountChangeEvent.setAmount(accountPay.getPayAmount());
        accountChangeEvent.setAccountNo(accountPay.getAccountNo());
        accountChangeEvent.setTxNo(accountPay.getId());
        accountInfoService.updateAccountBalance(accountChangeEvent);
        log.info("处理消息完成：{}", JSON.toJSONString(accountChangeEvent));
    }
}