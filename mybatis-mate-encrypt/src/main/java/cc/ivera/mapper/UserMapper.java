package cc.ivera.mapper;

import cc.ivera.config.SpiceBaseMapper;
import cc.ivera.entity.User;
import cc.ivera.entity.dto.UserDTO;
import cc.ivera.entity.vo.UserVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface UserMapper extends SpiceBaseMapper<User> {

    @Update("update user set password=#{u.password},email=#{u.email} where id=#{id}")
    Integer testUpdateById(@Param("id") Long id, @Param("u") User user);

    Integer insertBatchTest(@Param("userList") List<User> userList);

    Integer updateBatchUserById(@Param("userList") List<User> userList);

    List<UserVO> selectUserVOList(@Param("dto") UserDTO dto);

    UserVO selectUserDto(@Param("id") Long id);

    void testNotParams();
}
