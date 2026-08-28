package cc.ivera.userdemo.repo;

import cc.ivera.userdemo.domain.UserIndex;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPhoneIndexMapper {
  int upsert(Map<String, Object> p);
  UserIndex selectByPhone(Map<String, Object> p);
  Long selectUidByPhone(Map<String, Object> map);
}
