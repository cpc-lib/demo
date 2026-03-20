package cc.ivera.service.impl;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.transcoders.SerializingTranscoder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import cc.ivera.config.XMemcachedProperties;
import cc.ivera.domain.Book;
import cc.ivera.mapper.BookMapper;
import cc.ivera.service.BookService;

import java.util.HashMap;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;


    @Autowired
    private MemcachedClient memcachedClient;


    @Autowired
    private XMemcachedProperties xMemcachedProperties;


    private HashMap<Long, Book> cache = new HashMap<>();

    @Override
    public Book findByIdWithOutCache(Long id) {
        return bookMapper.findById(id);
    }


    @Override
    public Book findById_hashMap(Long id) {
        //如果当前缓存中没有本次要查询的数据，则进行查询，否则直接从缓存中获取数据返回
        Book book = cache.get(id);
        if (book == null) {
            book = bookMapper.findById(id);
            cache.put(id, book);
        }
        return book;
    }


    @Override
    @Cacheable(value = "ehcache", key = "#id")
    public Book findById_ehcache(Long id) {
        return bookMapper.findById(id);
    }

    @Override
    @Cacheable(value = "query_book_byId", key = "#id")
    public Book findById_redis(Long id) {
        return bookMapper.findById(id);
    }


    @Override
    public Book findById_memcache(Long id) {
        Book book = null;
        try {
            book = (Book) memcachedClient.get(xMemcachedProperties.getPrefix() + "book" + id);
            if (book == null) {
                memcachedClient.set(xMemcachedProperties.getPrefix() + "book" + id, 5, bookMapper.findById(id), new SerializingTranscoder());
                book = bookMapper.findById(id);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return book;
    }

}
