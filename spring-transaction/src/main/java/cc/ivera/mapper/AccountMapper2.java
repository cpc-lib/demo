package cc.ivera.mapper;

import org.apache.ibatis.annotations.Param;

public interface AccountMapper2 {
   void inMoney(@Param("name") String name, @Param("money")Integer money);
   void outMoney(@Param("name")String name,@Param("money")Integer money);
}
