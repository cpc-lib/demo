package cc.ivera.userdemo.service;

import cc.ivera.userdemo.api.dto.SearchUserRequest;
import cc.ivera.userdemo.api.dto.SearchUserResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserSearchService {

    private final RestHighLevelClient es;
    private final ObjectMapper om;

    @Value("${app.es.index:user_search_v1}")
    private String index;

    public SearchUserResponse search(SearchUserRequest req) {
        try {
            SearchSourceBuilder ssb = new SearchSourceBuilder();

            // ✅ tenantId 可为空：为空则不加过滤
            BoolQueryBuilder bq = QueryBuilders.boolQuery();
            if (!isBlank(req.getTenantId())) {
                bq.filter(QueryBuilders.termQuery("tenantId", req.getTenantId()));
            }

            // filters
            if (!isBlank(req.getGender())) {
                bq.filter(QueryBuilders.termQuery("gender", req.getGender()));
            }
            if (!isBlank(req.getStatus())) {
                bq.filter(QueryBuilders.termQuery("status", req.getStatus()));
            }
            if (req.getDeleted() != null) {
                bq.filter(QueryBuilders.termQuery("deleted", req.getDeleted()));
            }

            // keyword fuzzy
            if (!isBlank(req.getQ())) {
                String kw = req.getQ().trim();
                BoolQueryBuilder kwq = buildKeywordQuery(kw);
                bq.must(kwq);
            }

            ssb.query(bq);

            int size = (req.getPageSize() == null ? 20 : req.getPageSize());
            ssb.size(size);

            // stable sort for search_after
            ssb.sort("updatedAt", SortOrder.DESC);
            ssb.sort("uid", SortOrder.ASC);

            if (!isBlank(req.getNextToken())) {
                Object[] sa = decodeToken(req.getNextToken());
                ssb.searchAfter(sa);
            }

            SearchRequest sr = new SearchRequest(index);
            sr.source(ssb);

            SearchResponse resp = es.search(sr, RequestOptions.DEFAULT);

            List<SearchUserResponse.UserHit> list = new ArrayList<SearchUserResponse.UserHit>();
            Object[] lastSort = null;

            for (SearchHit h : resp.getHits().getHits()) {
                Map<String, Object> m = h.getSourceAsMap();
                list.add(new SearchUserResponse.UserHit(asString(m.get("tenantId")), asString(m.get("uid")), asString(m.get("phone")), asString(m.get("email")), asString(m.get("nickname")), asString(m.get("gender")), asString(m.get("status")), asString(m.get("updatedAt")), m.get("version") == null ? 0L : Long.parseLong(String.valueOf(m.get("version")))));
                lastSort = h.getSortValues();
            }

            String next = null;
            if (lastSort != null && list.size() == size) {
                next = encodeToken(lastSort);
            }
            return new SearchUserResponse(list, next);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据输入自动选择字段：
     * - 含 @：按 email_ngram 模糊（edge_ngram）
     * - 数字>=6：按 phone_ngram 模糊（edge_ngram）
     * - 其他：nickname + 兜底 email/phone
     */
    private BoolQueryBuilder buildKeywordQuery(String kw) {
        BoolQueryBuilder kwq = QueryBuilders.boolQuery().minimumShouldMatch(1);

        // email: u1@ -> 命中 u1@163.com/u1@gmail.com
        if (looksLikeEmail(kw)) {
            String low = kw.toLowerCase(Locale.ROOT);
            // 你的 mapping: email_ngram search_analyzer=keyword，非常适合前缀 token 匹配
            kwq.should(QueryBuilders.matchQuery("email_ngram", low).operator(Operator.OR));
            return kwq;
        }

        // phone: 允许 +86 138-0000-0000 / 138 0000 0000
        String digits = onlyDigits(kw);
        if (digits.length() >= 6) {
            kwq.should(QueryBuilders.matchQuery("phone_ngram", digits).operator(Operator.OR));
            return kwq;
        }

        // fallback: nickname + 兜底 email/phone
        kwq.should(QueryBuilders.matchQuery("nickname", kw).operator(Operator.OR));

        // 用户可能输入 "u1"（不含@），也希望搜到邮箱前缀
        String low = kw.toLowerCase(Locale.ROOT);
        kwq.should(QueryBuilders.matchQuery("email_ngram", low).operator(Operator.OR));

        if (digits.length() > 0) {
            kwq.should(QueryBuilders.matchQuery("phone_ngram", digits).operator(Operator.OR));
        }

        return kwq;
    }

    private String asString(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private String encodeToken(Object[] sortValues) throws Exception {
        String json = om.writeValueAsString(sortValues);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private Object[] decodeToken(String token) throws Exception {
        byte[] b = Base64.getUrlDecoder().decode(token);
        String json = new String(b, StandardCharsets.UTF_8);
        return om.readValue(json, Object[].class);
    }

    private boolean looksLikeEmail(String s) {
        return s != null && s.indexOf('@') >= 0;
    }

    private String onlyDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
