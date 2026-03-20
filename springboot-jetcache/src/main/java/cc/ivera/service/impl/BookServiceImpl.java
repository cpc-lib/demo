package cc.ivera.service.impl;

import com.alicp.jetcache.Cache;
import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import com.alicp.jetcache.anno.CreateCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.ivera.domain.Book;
import cc.ivera.mapper.BookMapper;
import cc.ivera.service.BookService;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

@Service
public class BookServiceImpl implements BookService {

    @Autowired
    private BookMapper bookMapper;

    @CreateCache(name="jetCache_",expire = 10,timeUnit = TimeUnit.SECONDS,cacheType = CacheType.REMOTE)
    private Cache<Long, Book> jetCache;


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
    //@Cacheable(value = "ehcache", key = "#id")
    public Book findById_ehcache(Long id) {
        return bookMapper.findById(id);
    }

    @Override
    //@Cacheable(value = "query_book_byId", key = "#id")
    public Book findById_redis(Long id) {
        return bookMapper.findById(id);
    }



    @Override
    public Book findById_jetcache(Long id) {
        Book book = jetCache.get(id);
        if(book==null){
            jetCache.put(id,bookMapper.findById(id));
            book=bookMapper.findById(id);
        }
        return book;
    }


    @Override
    @Cached(name="book_",expire=10,timeUnit=TimeUnit.SECONDS,cacheType=CacheType.REMOTE)
    public Book findById_jetcache2(Long id){
        return bookMapper.findById(id);
    }

}
