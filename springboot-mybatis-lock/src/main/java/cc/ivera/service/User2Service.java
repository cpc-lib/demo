package cc.ivera.service;



import cc.ivera.domain.User;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(readOnly=true)
public interface User2Service {

    @Transactional(readOnly=false)
    boolean save(User user);

    @Transactional(readOnly=false)
    boolean delete(Integer id);

    @Transactional(readOnly=false)
    boolean update(User user);

    List<User> findAll();

    User findById(Integer id);
}
