package cc.ivera.mapper;

import cc.ivera.entity.CartItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {

    @Select("select * from t_cart_item where cart_id = #{cartId} order by id")
    List<CartItem> selectByCartId(@Param("cartId") Long cartId);

    @Select("select * from t_cart_item where cart_id = #{cartId} and product_id = #{productId} limit 1")
    CartItem selectByCartAndProduct(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Select("select count(*) from t_cart_item where cart_id = #{cartId}")
    int countByCartId(@Param("cartId") Long cartId);

    @Delete("delete from t_cart_item where cart_id = #{cartId} and product_id = #{productId}")
    int deleteByCartAndProduct(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Delete("delete from t_cart_item where cart_id = #{cartId}")
    int deleteByCartId(@Param("cartId") Long cartId);
}
