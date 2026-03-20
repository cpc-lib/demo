package cc.ivera.service.impl;

import cc.ivera.config.DataSource;
import cc.ivera.config.ExecutorConfig;
import cc.ivera.config.SqlContext;
import cc.ivera.util.SqlUtils;
import cc.ivera.domain.Book;

import cc.ivera.mapper.BookMapper;
import cc.ivera.service.BookService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

@Service
@Slf4j
public class BookServiceImpl extends BaseServiceImpl implements BookService {

    @Resource
    private SqlContext sqlContext;

    @Autowired
    private BookMapper bookMapper2;

    /**
     * 使用10个线程插入100万条数据使用时间56秒，
     * 使用1个线程插入100万条数据使用时间37秒
     * 多线程插入确保事务，需要切换上下文操作，需要额外耗时。
     * 但是不处理事务的前提下，多线程处理快很多
     * 从sqlSession中获取mapper对象,自动控制提交事务的安全
     * @param bookList
     * @throws SQLException
     */
    @Override
    @DataSource("default")
    public void saveThreads(List<Book> bookList,Integer nThread) throws SQLException {
        // 获取数据库连接,获取会话(内部自有事务)
        SqlSession sqlSession = sqlContext.getSqlSession();
        Connection connection = sqlSession.getConnection();
        try {
            // 设置手动提交
            connection.setAutoCommit(false);
            BookMapper bookMapper = sqlSession.getMapper(BookMapper.class);
           //先做删除操作
            bookMapper.delete(null);
            ExecutorService service = ExecutorConfig.getThreadPool();
            List<Callable<Integer>> callableList  = new ArrayList<>();
            List<List<Book>> lists= SqlUtils.averageAssign(bookList, nThread);
            for (int i =0;i<lists.size();i++){
                List<Book> list  = lists.get(i);
                Callable<Integer> callable = () -> bookMapper.saveBatch(list);
                callableList.add(callable);
            }
            //执行子线程
            List<Future<Integer>> futures = service.invokeAll(callableList);
            for (Future<Integer> future:futures) {
                if (future.get()<=0){
                    connection.rollback();
                    return;
                }
            }
            connection.commit();
            System.out.println("添加完毕");
        }catch (Exception e){
            connection.rollback();
            log.info("error",e);
            throw new RuntimeException("出现异常");
        }
    }


    @Override
    @DataSource("default")
    public IPage<Book> getBookList(Long pageNum, Long pageSize) {
        Page pages = new Page(pageNum, pageSize);
        IPage<Book> page = bookMapper2.findPage(pages);
        return page;
    }


    @Override
    @DataSource("default")
    public Book selectOne(Long bookId){
        QueryWrapper<Book> wrapper=new QueryWrapper<>();
        wrapper.eq("book_id",bookId);
        Book book = bookMapper2.selectOne(wrapper);
        return book;
    }

    @Override
    @DataSource("default")
    public Book selectByPrimaryKey(Long bookId) {
        Book book = bookMapper2.selectById(bookId);
        return book;
    }

    @Override
    @DataSource("default")
    public Book select(Long bookId){
        LambdaQueryWrapper<Book> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(Book::getBookId, bookId);
        Book book = bookMapper2.selectOne(lambdaQueryWrapper);
        return book;
    }

    @Override
    @DataSource("default")
    public IPage<Book> selectPage(Long pageNum,Long pageSize){
        IPage page = new Page(pageNum,pageSize);
        page = bookMapper2.selectPage(page, null);
        return page;
    }

    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void insert(Book book) {
        bookMapper2.insert(book);
    }

    @Override
    @DataSource("default")
    public List<Book> selectList() {
        //最简单的查询
        return bookMapper2.selectList(null);
    }

    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateBatchA(List<Book> books) {
        bookMapper2.updateBatchA(books);
        //测试事务 经过测试是成功的
        //int i=1/0;
    }

    @Override
    @DataSource("default")
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public int updateBatchB(List<Book> books) {
        return bookMapper2.updateBatchB(books);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateA(Book book) {
        bookMapper2.updateC(book);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    public void updateB(Book book) {
        UpdateWrapper updateWrapper = new UpdateWrapper();
        updateWrapper.eq("book_id",book.getBookId());
        bookMapper2.update(book,updateWrapper);
    }


}
