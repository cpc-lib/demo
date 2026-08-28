package cc.ivera.userdemo.repo;

import cc.ivera.userdemo.domain.UserBase;
import java.util.Map;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserBaseMapper {
  int insert(Map<String, Object> p);
  UserBase selectAuthByUid(Map<String, Object> p);
  UserBase selectByUid(Map<String, Object> p);
  int updateProfile(Map<String, Object> p);
}
