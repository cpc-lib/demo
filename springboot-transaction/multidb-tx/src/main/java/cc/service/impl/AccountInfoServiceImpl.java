package cc.ivera.service.impl;

import cc.ivera.config.DataSource;
import cc.ivera.config.DynamicDataSource;
import cc.ivera.domain.AccountInfo;
import cc.ivera.mapper.AccountInfoMapper;
import cc.ivera.service.AccountInfoService;
import com.github.pagehelper.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 多数据源下继承mp的ServiceImpl报错，spring提示使用@Primary注解
 */
@Service
@Slf4j
public class AccountInfoServiceImpl extends BaseServiceImpl implements AccountInfoService {

   @Autowired
   private AccountInfoMapper accountInfoMapper;


    @Override
    public AccountInfo selectByNo(String accountNo) {
       return accountInfoMapper.selectByNo(accountNo);
    }

    @Override
    public Page<AccountInfo> selectPage1() {
        return accountInfoMapper.selectPage();
    }

    @Override
    @DataSource(value="platform")
    public Page<AccountInfo> selectPage2() {
        //查询数据库bank2
        return accountInfoMapper.selectPage();
    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public void insert1(AccountInfo accountInfo) {
        accountInfoMapper.insert1(accountInfo);
        accountInfoMapper.insert2(accountInfo);
        int i=1/0;

    }

    @Override
    @Transactional(propagation= Propagation.REQUIRED,rollbackFor=Exception.class)
    public void insert2(AccountInfo accountInfo) {
        DynamicDataSource.setDataSource("platform");//bank2数据库
        //调用BaseMapper的insert方法
        int count = accountInfoMapper.insert(accountInfo);
        DynamicDataSource.setDataSource("remote");//remote数据库
        accountInfoMapper.insert3(accountInfo);
        int i=1/0;
    }
}
