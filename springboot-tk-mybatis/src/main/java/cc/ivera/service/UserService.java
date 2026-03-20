package cc.ivera.service;


import org.springframework.transaction.annotation.Transactional;
import cc.ivera.domain.User;

import java.util.List;

@Transactional(readOnly=true)
public interface UserService {

    @Transactional(readOnly=false)
    boolean save(User user);

    @Transactional(readOnly=false)
    boolean delete(Integer id);

    @Transactional(readOnly=false)
    boolean update(User user);

    List<User> findAll();

    User findById(Long id);
}
