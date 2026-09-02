package cc.ivera.test;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


import java.io.IOException;

/**
 * @author Mr.M
 * @version 1.0
 * @description 使用Java Client 操作ES
 * @date 2024/8/18 17:40
 */
public class IndexTest {

    private ElasticsearchClient esClient;

    private RestClient restClient;

    @BeforeEach
    void setUp() {
        // Create the low-level client
        this.restClient = RestClient.builder(
                new HttpHost("192.168.1.200", 9200)).build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());


        // And create the API client
        this.esClient = new ElasticsearchClient(transport);
    }



    @AfterEach
    void tearDown() throws IOException {
        this.restClient.close();
    }

}