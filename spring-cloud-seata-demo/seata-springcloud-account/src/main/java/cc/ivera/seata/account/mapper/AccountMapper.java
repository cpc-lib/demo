package cc.ivera.seata.account.mapper;

import cc.ivera.seata.common.model.account.Account;

/**
 * 账户mapper
 * 
 * @author sly
 * @time 2019年6月12日
 */
public interface AccountMapper {

	/**
	 * 新增
	 * 
	 * @param account
	 * @return
	 * @author sly
	 * @time 2019年6月12日
	 */
	int insert(Account account);

}
