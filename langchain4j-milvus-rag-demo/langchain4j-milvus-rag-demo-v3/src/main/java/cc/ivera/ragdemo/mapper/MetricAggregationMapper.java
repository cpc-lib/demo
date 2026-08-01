package cc.ivera.ragdemo.mapper;

import cc.ivera.ragdemo.model.query.FeedbackQualityMetricUpsertCommand;
import cc.ivera.ragdemo.model.query.QueryCostMetricUpsertCommand;
import cc.ivera.ragdemo.model.query.RerankObservationMetricUpsertCommand;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
@InterceptorIgnore(tenantLine = "true")
public interface MetricAggregationMapper {

    List<Map<String, Object>> listWatermarks();

    List<Map<String, Object>> listQueryCostAggregationRows(@Param("from") LocalDateTime from,
                                                           @Param("to") LocalDateTime to);

    List<Map<String, Object>> listFeedbackQualityAggregationRows(@Param("from") LocalDateTime from,
                                                                 @Param("to") LocalDateTime to);

    List<Map<String, Object>> listRerankObservationAggregationRows(@Param("from") LocalDateTime from,
                                                                   @Param("to") LocalDateTime to);

    int upsertQueryCostMetric(QueryCostMetricUpsertCommand command);

    int upsertFeedbackQualityMetric(FeedbackQualityMetricUpsertCommand command);

    int upsertRerankObservationMetric(RerankObservationMetricUpsertCommand command);

    int upsertMetricAggregationWatermark(@Param("metric") String metric,
                                         @Param("watermark") LocalDateTime watermark);

    List<Map<String, Object>> listMaterializedQueryCostTrends(@Param("table") String table,
                                                              @Param("window") String window,
                                                              @Param("sourceWindow") String sourceWindow,
                                                              @Param("tenantId") Long tenantId,
                                                              @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                              @Param("queryType") String queryType,
                                                              @Param("retrievalMode") String retrievalMode,
                                                              @Param("status") String status,
                                                              @Param("llmModel") String llmModel,
                                                              @Param("embeddingModel") String embeddingModel,
                                                              @Param("from") LocalDateTime from,
                                                              @Param("to") LocalDateTime to);

    List<Map<String, Object>> listMaterializedFeedbackQualityTrends(@Param("table") String table,
                                                                    @Param("window") String window,
                                                                    @Param("sourceWindow") String sourceWindow,
                                                                    @Param("tenantId") Long tenantId,
                                                                    @Param("knowledgeBaseId") Long knowledgeBaseId,
                                                                    @Param("retrievalMode") String retrievalMode,
                                                                    @Param("queryType") String queryType,
                                                                    @Param("feedbackRating") String feedbackRating,
                                                                    @Param("feedbackStatus") String feedbackStatus,
                                                                    @Param("assignee") String assignee,
                                                                    @Param("from") LocalDateTime from,
                                                                    @Param("to") LocalDateTime to);

    List<Map<String, Object>> listMaterializedRerankObservationTrends(@Param("table") String table,
                                                                      @Param("window") String window,
                                                                      @Param("sourceWindow") String sourceWindow,
                                                                      @Param("tenantId") Long tenantId,
                                                                      @Param("provider") String provider,
                                                                      @Param("model") String model,
                                                                      @Param("apiKeyHash") String apiKeyHash,
                                                                      @Param("errorCode") String errorCode,
                                                                      @Param("degradedReason") String degradedReason,
                                                                      @Param("from") LocalDateTime from,
                                                                      @Param("to") LocalDateTime to);
}
