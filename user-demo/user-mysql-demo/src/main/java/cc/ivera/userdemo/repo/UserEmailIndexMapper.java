package cc.ivera.userdemo.repo;

import cc.ivera.userdemo.domain.UserIndex;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserEmailIndexMapper {
  int upsert(Map<String, Object> p);
  UserIndex selectByEmail(Map<String, Object> p);
  Long selectUidByEmail(Map<String, Object> p);
}
