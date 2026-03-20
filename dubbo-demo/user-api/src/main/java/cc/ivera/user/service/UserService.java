package cc.ivera.user.service;


import cc.ivera.user.domain.User;

public interface UserService {

    String queryUsername(Long id);

    User queryById(Long id);
}
