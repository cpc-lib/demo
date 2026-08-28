package cc.ivera.ragdemo.mapper;

import cc.ivera.ragdemo.domain.rag.RagQueryFeedback;
import cc.ivera.ragdemo.model.query.RagFeedbackRatingCount;
import cc.ivera.ragdemo.model.query.RagFeedbackSummaryItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RagQueryFeedbackMapper extends BaseMapper<RagQueryFeedback> {

    List<RagFeedbackRatingCount> summarizeFeedback(@Param("tenantId") Long tenantId,
                                                   @Param("queryType") String queryType,
                                                   @Param("status") String status,
                                                   @Param("conversationId") String conversationId,
                                                   @Param("traceId") String traceId,
                                                   @Param("rating") String rating,
                                                   @Param("createdBy") String createdBy);

    List<RagFeedbackSummaryItem> listRecentFeedback(@Param("tenantId") Long tenantId,
                                                    @Param("queryType") String queryType,
                                                    @Param("status") String status,
                                                    @Param("conversationId") String conversationId,
                                                    @Param("traceId") String traceId,
                                                    @Param("rating") String rating,
                                                    @Param("createdBy") String createdBy,
                                                    @Param("limit") int limit);
}
