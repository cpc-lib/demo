package cc.ivera.controller;


import cc.ivera.config.DynamicDataSource;
import com.baomidou.mybatisplus.core.metadata.IPage;
import cc.ivera.domain.AccountInfo;
import cc.ivera.domain.Book;
import cc.ivera.service.AccountInfoService;
import cc.ivera.service.BookService;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;


@RestController
public class BankController {

    @Autowired
    private AccountInfoService accountInfoService;


    @Autowired
    private BookService bookService;

    //测试切换数据源
    @GetMapping("/list0")
    public String list0() {
        DynamicDataSource.setDataSource("bank0");
        AccountInfo accountInfo2 = accountInfoService.selectByNo("5");
        System.out.println(accountInfo2);
        return "ok";
    }

    @GetMapping("/list1")
    public String list1() {
        DynamicDataSource.setDataSource("default");
        AccountInfo accountInfo2 = accountInfoService.selectByNo("1");
        System.out.println(accountInfo2);
        return "ok";
    }

    @GetMapping("/list2")
    public String list2() {
        DynamicDataSource.setDataSource("platform");
        AccountInfo accountInfo2 = accountInfoService.selectByNo("2");
        System.out.println(accountInfo2);
        return "ok";
    }

    //测试pagehelper的分页
    @GetMapping("/pagehelper/{pageNum}/{pageSize}")
    public String page1(@PathVariable("pageNum") Integer pageNum, @PathVariable("pageSize") Integer pageSize) {
        DynamicDataSource.setDataSource("default");//查询数据库bank1
        PageHelper.startPage(pageNum, pageSize);
        Page<AccountInfo> page = accountInfoService.selectPage1();
        PageInfo<AccountInfo> pageInfo = new PageInfo(page);
        System.out.println(pageInfo);
        System.out.println(page);
        return "ok";
    }

    //测试使用注解切换数据源
    @GetMapping("/pagehelper/{pageno}/{pagesize}")
    public String page2(@PathVariable("pageno") Integer pageNum, @PathVariable("pagesize") Integer pageSize) {
        PageHelper.startPage(pageNum, pageSize);
        Page<AccountInfo> page = accountInfoService.selectPage2();
        PageInfo<AccountInfo> pageInfo = new PageInfo(page);
        System.out.println(pageInfo);
        System.out.println(page);
        return "ok";
    }


    //测试事务(使用本地的两个数据库)
    @GetMapping("/dk")
    public String insert1() {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccountName("dk");
        accountInfo.setAccountBalance(50.00);
        accountInfo.setAccountNo("100");
        try {
            accountInfoService.insert1(accountInfo);
            return "ok";
        } catch (Exception e) {
            return "fail";
        }


    }

    //测试事务(使用本地的一个与线上的一个数据库)
    @GetMapping("/gen")
    public String insert2() {
        AccountInfo accountInfo = new AccountInfo();
        accountInfo.setAccountName("gen");
        accountInfo.setAccountBalance(50.00);
        accountInfo.setAccountNo("100");
        try {
            accountInfoService.insert2(accountInfo);
            return "ok";
        } catch (Exception e) {
            return "fail";
        }
    }


    @GetMapping("/multithread/{nThread}")
    public String insert3(@PathVariable("nThread") Integer nThread) {
        if (nThread >= 1 && nThread <= 10) {
            long begin = System.currentTimeMillis();
            int size = 10000;
            String result = "";
            List<Book> bookList = new ArrayList<Book>(size);
            for (long i = 0; i < size; i++) {
                Book book = new Book();
                book.setBookId(i + 1);
                book.setBookName("book" + i + 1);
                book.setBookAuthorName("bookauthor" + i + 1);
                bookList.add(book);
            }
            try {
                bookService.saveThreads(bookList, nThread);
                long end = System.currentTimeMillis();
                result = (end - begin) + "ms";
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                return result;
            }
        } else {
            return "Too many to create is not allowed";
        }
    }


    /**
     * 测试mp的Page,IPage分页控制
     * @param pageNum
     * @param pageSize
     * @return
     */
    @GetMapping("/ipage/{pageNum}/{pageSize}")
    public String ipage(@PathVariable("pageNum") Long pageNum, @PathVariable("pageSize") Long pageSize) {
        IPage<Book> bookList = bookService.getBookList(pageNum, pageSize);
        System.out.println(bookList.getRecords());
        System.out.println("----------------------------------");
        System.out.println(bookList.getCurrent());
        System.out.println("----------------------------------");
        System.out.println(bookList.getPages());
        System.out.println("----------------------------------");
        System.out.println(bookList.getSize());
        System.out.println("----------------------------------");
        System.out.println(bookList.getTotal());
        System.out.println("----------------------------------");
        return "ok";
    }


}
