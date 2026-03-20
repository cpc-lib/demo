package cc.ivera.mapper;

import cc.ivera.pojo.User;

import java.util.List;

public interface UserMapper {

    int save(User user);

    int delete(Integer id);

    int update(User user);

    List<User> findAll();

    User findById(Integer id);
}
