package cc.ivera.mapper;

import cc.ivera.entity.MessageConsumeLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Date;

@Mapper
public interface MessageConsumeLogMapper {

    @Insert("insert into t_message_consume_log "
            + "(event_id, consumer_name, event_type, business_key, status, locked_by, lock_expire_time) "
            + "values (#{log.eventId}, #{log.consumerName}, #{log.eventType}, #{log.businessKey}, "
            + "#{log.status}, #{log.lockedBy}, timestampadd(second, #{leaseSeconds}, now()))")
    int insert(@Param("log") MessageConsumeLog log, @Param("leaseSeconds") long leaseSeconds);

    @Select("select * from t_message_consume_log "
            + "where event_id = #{eventId} and consumer_name = #{consumerName} limit 1 for update")
    MessageConsumeLog selectByEventAndConsumer(@Param("eventId") String eventId,
                                               @Param("consumerName") String consumerName);

    @Update("update t_message_consume_log set status = 'PROCESSING', locked_by = #{lockedBy}, "
            + "lock_expire_time = timestampadd(second, #{leaseSeconds}, now()), last_error = null "
            + "where event_id = #{eventId} and consumer_name = #{consumerName} "
            + "and ((status = 'FAILED' and (lock_expire_time is null or lock_expire_time <= now())) "
            + "or (status = 'PROCESSING' and (lock_expire_time is null or lock_expire_time <= now())))")
    int reclaimExpiredOrFailed(@Param("eventId") String eventId,
                               @Param("consumerName") String consumerName,
                               @Param("lockedBy") String lockedBy,
                               @Param("leaseSeconds") long leaseSeconds);

    @Update("update t_message_consume_log set status = 'CONSUMED', consumed_at = #{consumedAt}, "
            + "locked_by = null, lock_expire_time = null, last_error = null "
            + "where event_id = #{eventId} and consumer_name = #{consumerName} "
            + "and status = 'PROCESSING' and locked_by = #{lockedBy}")
    int markConsumed(@Param("eventId") String eventId,
                     @Param("consumerName") String consumerName,
                     @Param("lockedBy") String lockedBy,
                     @Param("consumedAt") Date consumedAt);

    @Update("update t_message_consume_log set status = 'FAILED', last_error = #{lastError}, "
            + "locked_by = null, lock_expire_time = #{retryAfter} "
            + "where event_id = #{eventId} and consumer_name = #{consumerName} "
            + "and status = 'PROCESSING' and locked_by = #{lockedBy}")
    int markFailed(@Param("eventId") String eventId,
                   @Param("consumerName") String consumerName,
                   @Param("lockedBy") String lockedBy,
                   @Param("lastError") String lastError,
                   @Param("retryAfter") Date retryAfter);
}
