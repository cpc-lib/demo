package cc.ivera.mapper;

import cc.ivera.entity.MessageOutbox;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

public interface MessageOutboxMapper extends BaseMapper<MessageOutbox> {

    MessageOutbox selectByEventKey(@Param("eventKey") String eventKey);

    MessageOutbox selectByEventId(@Param("eventId") String eventId);

    List<MessageOutbox> selectPublishable(@Param("limit") int limit);

    int claimForPublish(@Param("id") Long id,
                        @Param("lockedBy") String lockedBy,
                        @Param("leaseSeconds") long leaseSeconds);

    int markSent(@Param("id") Long id,
                 @Param("lockedBy") String lockedBy,
                 @Param("sentAt") Date sentAt);

    int markFailed(@Param("id") Long id,
                   @Param("lockedBy") String lockedBy,
                   @Param("lastError") String lastError,
                   @Param("nextRetryTime") Date nextRetryTime);

    int resetFailed(@Param("eventId") String eventId);
}
