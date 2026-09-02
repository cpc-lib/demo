package cc.ivera.mapper;

import cc.ivera.entity.OrderIdempotency;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

public interface OrderIdempotencyMapper extends BaseMapper<OrderIdempotency> {

    OrderIdempotency selectByKeyForUpdate(@Param("idempotencyKey") String idempotencyKey);

    int expireIssuedKeyIfNeeded(@Param("userId") Long userId,
                                @Param("idempotencyKey") String idempotencyKey,
                                @Param("now") Date now);

    int completeIssued(@Param("id") Long id,
                       @Param("requestFingerprint") String requestFingerprint,
                       @Param("orderId") Long orderId,
                       @Param("completedAt") Date completedAt);

    int deleteUnusedExpiredBefore(@Param("cutoff") Date cutoff);
}
