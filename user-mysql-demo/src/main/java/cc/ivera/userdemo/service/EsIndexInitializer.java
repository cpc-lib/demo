package cc.ivera.userdemo.service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EsIndexInitializer implements ApplicationRunner {

  private final RestHighLevelClient es;

  @Value("${app.es.index:user_search_v1}")
  private String index;

  @Override
  public void run(ApplicationArguments args) throws Exception {
    boolean exists = es.indices().exists(new GetIndexRequest(index), RequestOptions.DEFAULT);
    if (exists) {
      log.info("ES index exists: {}", index);
      return;
    }
    String mapping = readClasspath("es/user_search_v1.json");
    CreateIndexRequest req = new CreateIndexRequest(index);
    req.source(mapping, XContentType.JSON);
    es.indices().create(req, RequestOptions.DEFAULT);
    log.info("ES index created: {}", index);
  }

  private String readClasspath(String path) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
      if (in == null) throw new IllegalStateException("missing resource: " + path);
      byte[] b = in.readAllBytes();
      return new String(b, StandardCharsets.UTF_8);
    }
  }
}
