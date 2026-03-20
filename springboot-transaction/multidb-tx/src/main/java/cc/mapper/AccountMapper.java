package cc.ivera.mapper;

import cc.ivera.domain.Account;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface AccountMapper extends IBaseMapper<Account> {

}
