package cc.ivera.userdemo.service;

import cc.ivera.userdemo.config.AppProperties;
import cc.ivera.userdemo.domain.UserProfile;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.VersionType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EsUserIndexService {

    private final RestHighLevelClient es;
    private final AppProperties props;

    public void upsert(UserProfile p) throws IOException {
        Map<String, Object> doc = new HashMap<>();
        doc.put("uid", p.getUid());
        doc.put("phone", p.getPhone());
        doc.put("email", p.getEmail());
        doc.put("status", p.getStatus());
        doc.put("deleted", p.getDeleted());
        doc.put("version", p.getVersion());
        doc.put("createdAt", p.getCreatedAt());
        doc.put("updatedAt", p.getUpdatedAt());
        doc.put("canceledAt", p.getCanceledAt());

        IndexRequest req = new IndexRequest(props.getEs().getIndex())
                .id(p.getUid())
                .source(doc)
                .version(p.getVersion())
                .versionType(VersionType.EXTERNAL_GTE);

        es.index(req, RequestOptions.DEFAULT);
    }

    //  public Map<String, Object> search(String q, int page, int size) throws IOException {
//    int from = Math.max(0, (page - 1) * size);
//
//    BoolQueryBuilder bq = QueryBuilders.boolQuery();
//    if (q != null && !q.isBlank()) {
//      // phone/email 精确匹配优先；否则走 keyword-like 的多字段查询
//      bq.should(QueryBuilders.termQuery("phone", q));
//      bq.should(QueryBuilders.termQuery("email", q.toLowerCase()));
//      bq.should(QueryBuilders.matchQuery("uid", q));
//      bq.minimumShouldMatch(1);
//    } else {
//      bq.must(QueryBuilders.matchAllQuery());
//    }
//
//    SearchSourceBuilder ssb = new SearchSourceBuilder()
//        .query(bq)
//        .from(from)
//        .size(size)
//        .sort("updatedAt", SortOrder.DESC)
//        .sort("uid", SortOrder.ASC);
//
//    SearchRequest req = new SearchRequest(props.getEs().getIndex()).source(ssb);
//    SearchResponse resp = es.search(req, RequestOptions.DEFAULT);
//
//    List<Map<String, Object>> items = new ArrayList<>();
//    for (SearchHit hit : resp.getHits().getHits()) {
//      Map<String, Object> m = hit.getSourceAsMap();
//      m.put("_id", hit.getId());
//      items.add(m);
//    }
//
//    Map<String, Object> out = new LinkedHashMap<>();
//    out.put("total", resp.getHits().getTotalHits().value);
//    out.put("page", page);
//    out.put("size", size);
//    out.put("items", items);
//    return out;
//  }
    public Map<String, Object> search(String q, String phone, String email, int page, int size) throws IOException {
        int from = Math.max(0, (page - 1) * size);

        BoolQueryBuilder mainQuery = QueryBuilders.boolQuery();

        boolean hasPhone = phone != null && !phone.trim().isEmpty();
        boolean hasEmail = email != null && !email.trim().isEmpty();
        boolean hasQ = q != null && !q.trim().isEmpty();

        if (hasPhone || hasEmail) {
            // 精确 AND 查询：phone 和/或 email
            if (hasPhone) {
                mainQuery.must(QueryBuilders.termQuery("phone", phone.trim()));
            }
            if (hasEmail) {
                mainQuery.must(QueryBuilders.termQuery("email", email.trim().toLowerCase()));
            }
        } else if (hasQ) {
            // phone 和 email 都为空，使用 q 做模糊 OR 查询
            String qTrim = q.trim();
            String qLower = qTrim.toLowerCase();

            BoolQueryBuilder shouldQuery = QueryBuilders.boolQuery();
            //shouldQuery.should(QueryBuilders.wildcardQuery("phone", "*" + qTrim + "*"));
            //shouldQuery.should(QueryBuilders.wildcardQuery("email", "*" + qLower + "*"));

            // 使用 prefixQuery 替代 wildcard，性能更好
            shouldQuery.should(QueryBuilders.prefixQuery("phone", qTrim));
            shouldQuery.should(QueryBuilders.prefixQuery("email", qLower));

            //shouldQuery.should(QueryBuilders.matchQuery("uid", qTrim));
            shouldQuery.minimumShouldMatch(1);

            mainQuery.must(shouldQuery);
        }
        // 如果三者都为空，mainQuery 保持为空 → 相当于 match_all

        SearchSourceBuilder ssb = new SearchSourceBuilder()
                .query(mainQuery)
                .from(from)
                .size(size)
                .sort("updatedAt", SortOrder.DESC)
                .sort("uid", SortOrder.ASC);

        SearchRequest req = new SearchRequest(props.getEs().getIndex()).source(ssb);
        SearchResponse resp = es.search(req, RequestOptions.DEFAULT);

        List<Map<String, Object>> items = new ArrayList<>();
        for (SearchHit hit : resp.getHits().getHits()) {
            Map<String, Object> m = hit.getSourceAsMap();
            m.put("_id", hit.getId());
            items.add(m);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", resp.getHits().getTotalHits().value);
        out.put("page", page);
        out.put("size", size);
        out.put("items", items);
        return out;
    }
}
