package cc.ivera.mapper;

import cc.ivera.domain.Goods;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface GoodsMapper {
    List<Goods> findAll();
}
