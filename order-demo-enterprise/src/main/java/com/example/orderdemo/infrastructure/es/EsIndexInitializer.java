package com.example.orderdemo.infrastructure.es;

import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Slf4j
@Component
public class EsIndexInitializer {

    private final RestHighLevelClient client;
    private final String index;

    public EsIndexInitializer(RestHighLevelClient client,
                              @Value("${app.es.index}") String index) {
        this.client = client;
        this.index = index;
    }

    public void ensureIndex() throws Exception {
        boolean exists = client.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT);
        if (exists) {
            log.info("ES index exists: {}", index);
            return;
        }

        String mappingJson = new String(new ClassPathResource("docs/es/order_v1.json")
                .getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        CreateIndexRequest req = new CreateIndexRequest(index);
        req.source(mappingJson, XContentType.JSON);

        client.indices().create(req, RequestOptions.DEFAULT);
        log.info("ES index created: {}", index);
    }
}
