package cc.ivera.service;

import cc.ivera.domain.User;


public interface UserService {

    User findByIdWithOutCache(Long id);

    User findById_hashMap(Long id);

    User findById_ecache(Long id);

    User findById_redis(Long id);

    User findById_memcache(Long id);
}
