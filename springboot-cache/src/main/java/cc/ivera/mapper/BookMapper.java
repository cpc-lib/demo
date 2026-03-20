package cc.ivera.mapper;

import org.apache.ibatis.annotations.Mapper;
import cc.ivera.domain.Book;


@Mapper
public interface BookMapper extends MyBaseMapper<Book> {

   Book findById(Long id);
}
