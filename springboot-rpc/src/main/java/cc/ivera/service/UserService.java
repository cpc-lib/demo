package cc.ivera.service;

import cc.ivera.pojo.User;
import com.github.pagehelper.PageInfo;

public interface UserService {

    boolean save(User user);

    boolean delete(Integer id);

    boolean update(User user);

    PageInfo<User> findAll(Integer page, Integer size);

    User findById(Integer id);
}
