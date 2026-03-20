package cc.ivera.service.impl;

import cc.ivera.config.DataSource;
import cc.ivera.domain.Account;
import cc.ivera.mapper.AccountMapper;
import cc.ivera.service.AccountService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountServiceImpl extends BaseServiceImpl implements AccountService {


    @Autowired
    private AccountMapper accountMapper;


    @Override
    @DataSource("default")
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public int insert(Account account) {
        return accountMapper.insert(account);
    }

}
