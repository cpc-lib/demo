package cc.ivera.dubbo.api;


import cc.ivera.dubbo.domain.User;

public interface UserService {

    User queryById(Long id);
}
