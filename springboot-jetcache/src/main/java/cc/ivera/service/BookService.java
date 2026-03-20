package cc.ivera.service;

import cc.ivera.domain.Book;

public interface BookService {

    Book findByIdWithOutCache(Long id);

    Book findById_hashMap(Long id);

    Book findById_ehcache(Long id);

    Book findById_redis(Long id);

    Book findById_jetcache(Long id);

    Book findById_jetcache2(Long id);
}
