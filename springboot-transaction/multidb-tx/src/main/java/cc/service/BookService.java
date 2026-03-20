package cc.ivera.service;

import cc.ivera.domain.Book;
import com.baomidou.mybatisplus.core.metadata.IPage;


import java.sql.SQLException;
import java.util.List;

public interface BookService {

    public void saveThreads(List<Book> bookList, Integer n) throws SQLException;

    public IPage<Book> getBookList(Long pageNum, Long pageSize);

    public Book selectOne(Long bookId);

    public Book selectByPrimaryKey(Long bookId);

    public Book select(Long bookId);

    public IPage<Book> selectPage(Long pageNum, Long pageSize);

    public void insert(Book book);

    public List<Book> selectList();

    public void updateBatchA(List<Book> books);

    public int updateBatchB(List<Book> books);

    public void updateA(Book book);

    public void updateB(Book book);

}
