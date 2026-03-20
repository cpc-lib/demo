package cc.ivera.dtx.txmsgdemo.bank2.service.impl;

import cc.ivera.dtx.txmsgdemo.bank2.dao.AccountInfoDao;
import cc.ivera.dtx.txmsgdemo.bank2.model.AccountChangeEvent;
import cc.ivera.dtx.txmsgdemo.bank2.service.AccountInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class AccountInfoServiceImpl implements AccountInfoService {

    @Autowired
    AccountInfoDao accountInfoDao;

    private static final ConcurrentHashMap<String, AtomicInteger> cache = new ConcurrentHashMap<>();

    /**
     * 消费消息，更新本地事务，添加金额
     *
     * @param accountChangeEvent
     */
    @Override
    @Transactional
    public void addAccountInfoBalance(AccountChangeEvent accountChangeEvent) {
        AtomicInteger atomicInteger = cache.get(accountChangeEvent.getTxNo());
        if (atomicInteger == null) {
            atomicInteger = new AtomicInteger(1);
        }
        log.info("bank2更新本地账号，账号：{},金额： {}", accountChangeEvent.getAccountNo(), accountChangeEvent.getAmount());
        //幂等校验(判断是否有数据)
        if (accountInfoDao.isExistTx(accountChangeEvent.getTxNo()) > 0) {
            return;
        }
        //执行更新
        accountInfoDao.updateAccountBalance(accountChangeEvent.getAccountNo(), accountChangeEvent.getAmount());
        //添加事务记录
        accountInfoDao.addTx(accountChangeEvent.getTxNo());
        if (accountChangeEvent.getAmount() == 4) {
            int count = atomicInteger.addAndGet(1);
            atomicInteger = new AtomicInteger(count);
            cache.put(accountChangeEvent.getTxNo(), atomicInteger);
            if (count < 3) {
                throw new RuntimeException("人为制造异常");
            }else{
                log.info("处理成功");
            }
        }
    }
}
