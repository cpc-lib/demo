package cc.ivera.mapper;

import cc.ivera.domain.Good;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

@Repository
@Mapper
public interface GoodMapper extends IBaseMapper<Good> {

    //     <update id="updateStick" parameterType="com.example.demo.entity.GoodsEntity">
//    update goods set goods_name=#{goodsName,jdbcType=VARCHAR},stock=#{stock,jdbcType=INTEGER},version=version+1,update_time=#{updateTime,jdbcType=TIMESTAMP} where version=#{version,jdbcType=INTEGER} AND id=#{id,jdbcType=INTEGER}
// </update>
    public Integer updateGoodTotal(@Param("total") Long total,
                                   @Param("version") Long version,
                                   @Param("goodId") String goodId);

    Good selectByGoodId(@Param("goodId")String goodId);

    //Integer updateByGoodId(@Param("goodId") String goodId);

    Integer updateByGood(Good good);
}
