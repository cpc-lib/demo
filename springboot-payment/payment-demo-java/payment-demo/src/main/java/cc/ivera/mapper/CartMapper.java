package cc.ivera.mapper;

import cc.ivera.entity.Cart;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CartMapper extends BaseMapper<Cart> {

    @Select("select * from t_cart where user_id = #{userId} limit 1")
    Cart selectByUserId(@Param("userId") Long userId);

    @Select("select * from t_cart where user_id = #{userId} limit 1 for update")
    Cart selectByUserIdForUpdate(@Param("userId") Long userId);
}
