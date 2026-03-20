package cc.ivera.mapper;

import cc.ivera.domain.Book;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @Repository 可有可无
 * @Mapper 在启动类上添加了包路径扫描，可被扫描到，可以省略
 */
@Repository
@Mapper
public interface BookMapper extends IBaseMapper<Book> {

    public int saveBatch(List<Book> bookList);

    @Select("select * from book")
    IPage<Book> findPage(@Param("pages") Page pages);

    void updateBatchA(List<Book> books);

    int updateBatchB(List<Book> books);

    void updateC(Book book);
}
