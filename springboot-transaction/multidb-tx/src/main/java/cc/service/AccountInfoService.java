package cc.ivera.service;


import cc.ivera.domain.AccountInfo;
import com.github.pagehelper.Page;


public interface AccountInfoService {

   AccountInfo selectByNo(String accountNo);

   Page<AccountInfo> selectPage1();

   Page<AccountInfo> selectPage2();

   void insert1(AccountInfo accountInfo);

   void insert2(AccountInfo accountInfo);

}
