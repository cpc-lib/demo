package cc.ivera.mapper;

import cc.ivera.entity.RefreshToken;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface RefreshTokenMapper extends BaseMapper<RefreshToken> {

    @Select("select * from t_refresh_token where token_hash = #{tokenHash} limit 1 for update")
    RefreshToken selectByHashForUpdate(@Param("tokenHash") String tokenHash);

    @Update("update t_refresh_token set revoked_at = #{revokedAt}, update_time = now() " +
            "where token_family = #{tokenFamily} and revoked_at is null")
    int revokeFamily(@Param("tokenFamily") String tokenFamily, @Param("revokedAt") Date revokedAt);

    @Update("update t_refresh_token set revoked_at = #{revokedAt}, update_time = now() " +
            "where user_id = #{userId} and revoked_at is null")
    int revokeAllByUser(@Param("userId") Long userId, @Param("revokedAt") Date revokedAt);
}
