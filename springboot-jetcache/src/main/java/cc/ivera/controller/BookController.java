package cc.ivera.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.domain.Book;
import cc.ivera.service.BookService;

@RestController
@RequestMapping("/book")
public class BookController {

    @Autowired
    private BookService bookService;



    @GetMapping("/{id}")
    public Book findByIdWithOutCache(@PathVariable("id") Long id) {
        return bookService.findByIdWithOutCache(id);
    }


    /*支持缓存的接口*/

    @GetMapping("/hashMap/{id}")
    public Book findById_hashMap(@PathVariable("id") Long id){
        return bookService.findById_hashMap(id);
    }


    @GetMapping("/ehcache/{id}")
    public Book findById_ecache(@PathVariable("id") Long id){
        return bookService.findById_ehcache(id);
    }

    @GetMapping("/redis/{id}")
    public Book findById_redis(@PathVariable("id") Long id){
        return bookService.findById_redis(id);
    }


    @GetMapping("/jetcache/{id}")
    public Book findById_jetcache(@PathVariable("id") Long id){
        return bookService.findById_jetcache(id);
    }

    @GetMapping("/jetcache2/{id}")
    public Book findById_jetcache2(@PathVariable("id") Long id){
        return bookService.findById_jetcache2(id);
    }
}
