package com.example.orderdemo.infrastructure.es;

import com.example.orderdemo.domain.event.OrderEsDocument;
import com.example.orderdemo.infrastructure.json.Jsons;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Repository
public class OrderEsRepository {

  private final RestHighLevelClient client;
  private final String index;

  public OrderEsRepository(RestHighLevelClient client, @Value("${app.es.index}") String index) {
    this.client = client;
    this.index = index;
  }

  public void upsert(OrderEsDocument doc) throws Exception {
    String id = doc.getOrderId();

    UpdateRequest req = new UpdateRequest(index, id);
    req.doc(Jsons.toJson(doc), XContentType.JSON);
    req.docAsUpsert(true);

    UpdateResponse resp = client.update(req, RequestOptions.DEFAULT);
    log.info("ES upsert orderId={}, result={}", id, resp.getResult());
  }

  public SearchResult search(
          Long userId,
          String status,
          Long minTotalAmount,
          Long maxTotalAmount,
          LocalDateTime createdAtFrom,
          LocalDateTime createdAtTo,
          int page,
          int size
  ) throws Exception {

    BoolQueryBuilder bool = QueryBuilders.boolQuery();

    // 精确过滤：userId / status
    if (userId != null) {
      bool.filter(QueryBuilders.termQuery("userId", String.valueOf(userId)));
    }
    if (status != null && !status.isBlank()) {
      bool.filter(QueryBuilders.termQuery("status", status));
    }

    // 金额范围过滤：totalAmount
    if (minTotalAmount != null || maxTotalAmount != null) {
      var range = QueryBuilders.rangeQuery("totalAmount");
      if (minTotalAmount != null) range.gte(minTotalAmount);
      if (maxTotalAmount != null) range.lte(maxTotalAmount);
      bool.filter(range);
    }

    // 下单时间范围过滤：createdAt
    if (createdAtFrom != null || createdAtTo != null) {
      var range = QueryBuilders.rangeQuery("createdAt");
      if (createdAtFrom != null) range.gte(createdAtFrom);
      if (createdAtTo != null) range.lte(createdAtTo);
      bool.filter(range);
    }

    SearchSourceBuilder source = new SearchSourceBuilder()
            .query(bool)
            .from(page * size)
            .size(size)
            .sort("createdAt", SortOrder.DESC);

    SearchRequest req = new SearchRequest(index).source(source);
    var resp = client.search(req, RequestOptions.DEFAULT);

    List<OrderEsDocument> list = new ArrayList<>();
    for (SearchHit hit : resp.getHits().getHits()) {
      list.add(Jsons.fromJson(hit.getSourceAsString(), OrderEsDocument.class));
    }
    return new SearchResult(resp.getHits().getTotalHits().value, list);
  }


  public static class SearchResult {
    public final long total;
    public final List<OrderEsDocument> docs;
    public SearchResult(long total, List<OrderEsDocument> docs) {
      this.total = total;
      this.docs = docs;
    }
  }
}
