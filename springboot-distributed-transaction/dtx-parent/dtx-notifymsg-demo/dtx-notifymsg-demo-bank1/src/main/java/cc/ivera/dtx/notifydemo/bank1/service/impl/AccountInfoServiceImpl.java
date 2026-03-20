package cc.ivera.dtx.notifydemo.bank1.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import cc.ivera.dtx.notifydemo.bank1.dao.AccountInfoDao;
import cc.ivera.dtx.notifydemo.bank1.entity.AccountPay;
import cc.ivera.dtx.notifydemo.bank1.feign.PayClient;
import cc.ivera.dtx.notifydemo.bank1.model.AccountChangeEvent;
import cc.ivera.dtx.notifydemo.bank1.service.AccountInfoService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountInfoServiceImpl implements AccountInfoService {
    @Autowired
    AccountInfoDao accountInfoDao;

    @Autowired
    PayClient payClient;

    /**
     * 更新帐号余额,并发送消息
     *
     * @param accountChange
     */
    @Override
    @Transactional
    public void updateAccountBalance(AccountChangeEvent accountChange) {
        //幂等校验
        int existTx = accountInfoDao.isExistTx(accountChange.getTxNo());
        if (existTx > 0) {
            log.info("已处理消息：{}", JSONObject.toJSONString(accountChange));
            return;
        }
        //添加事务记录
        accountInfoDao.addTx(accountChange.getTxNo());
        //更新账户金额
        accountInfoDao.updateAccountBalance(accountChange.getAccountNo(), accountChange.getAmount());
    }

    /**
     * 主动查询充值结果
     *
     * @param tx_no
     */
    @Override
    public AccountPay queryPayResult(String tx_no) {
        //主动请求充值系统查询充值结果
        AccountPay accountPay = payClient.queryPayResult(tx_no);
        //充值结果
        String result = accountPay.getResult();
        log.info("主动查询充值结果：{}", JSON.toJSONString(accountPay));
        if ("success".equals(result)) {
            AccountChangeEvent accountChangeEvent = new AccountChangeEvent();
            accountChangeEvent.setAccountNo(accountPay.getAccountNo());
            accountChangeEvent.setAmount(accountPay.getPayAmount());
            accountChangeEvent.setTxNo(accountPay.getId());
            //以下两种写法都可以
            //updateAccountBalance(accountChangeEvent);
            getSelf().updateAccountBalance(accountChangeEvent);
        }
        return accountPay;
    }

    private AccountInfoServiceImpl getSelf() {
        return SpringUtil.getBean(getClass());
    }
}
