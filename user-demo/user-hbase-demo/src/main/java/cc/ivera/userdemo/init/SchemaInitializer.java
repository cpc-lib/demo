package cc.ivera.userdemo.init;

import cc.ivera.userdemo.config.AppProperties;
import cn.hutool.extra.spring.SpringUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.apache.hadoop.hbase.util.Bytes;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaInitializer implements CommandLineRunner {

    private final AppProperties props;
    private final Connection hbaseConn;
    private final RestHighLevelClient es;

    @Override
    public void run(String... args) throws Exception {
        if (props.getHbase().isInit()) initHBase();
        if (props.getEs().isInit()) initEs();
    }

    public void initHBase() throws Exception {
        String namespace = "u";
        boolean nsExists;
        try (Admin admin = hbaseConn.getAdmin()) {
            try {
                admin.getNamespaceDescriptor(namespace);
                nsExists = true;
            } catch (Exception e) {
                nsExists = false;
            }
            if (!nsExists) {
                admin.createNamespace(NamespaceDescriptor.create(namespace).build());
                log.info("Created namespace {}", namespace);
            }
            getSelf().createIfAbsent(admin, "u:user");
            getSelf().createIfAbsent(admin, "u:uniq_phone");
            getSelf().createIfAbsent(admin, "u:uniq_email");
        }
    }

    public void createIfAbsent(Admin admin, String table) throws Exception {
        TableName tn = TableName.valueOf(table);
        if (admin.tableExists(tn)) {
            log.info("HBase table exists: {}", table);
            return;
        }
        var td = TableDescriptorBuilder.newBuilder(tn)
                .setColumnFamily(ColumnFamilyDescriptorBuilder.newBuilder(Bytes.toBytes("cf"))
                        .setMaxVersions(1)
                        .build())
                .build();

        byte[][] splits = hexSplits();
        admin.createTable(td, splits);
        log.info("HBase table created: {} splits={}", table, splits.length);
    }

    private byte[][] hexSplits() {
        // 00..ff 的分区：split keys 取 01..ff
        byte[][] splits = new byte[255][];
        for (int i = 1; i <= 255; i++) {
            String hex = String.format("%02x", i);
            splits[i - 1] = Bytes.toBytes(hex);
        }
        return splits;
    }

//    public void initEs() throws Exception {
//        String index = props.getEs().getIndex();
//        GetIndexRequest get = new GetIndexRequest(index);
//        if (es.indices().exists(get, RequestOptions.DEFAULT)) {
//            log.info("ES index exists: {}", index);
//            return;
//        }
//
//        CreateIndexRequest req = new CreateIndexRequest(index);
//        req.settings(Settings.builder()
//                .put("number_of_shards", 3)
//                .put("number_of_replicas", 0));
//
//        String mapping = "{\n" +
//                "  \"dynamic\": \"strict\",\n" +
//                "  \"properties\": {\n" +
//                "    \"uid\": {\"type\": \"keyword\"},\n" +
//                "    \"phone\": {\"type\": \"keyword\"},\n" +
//                "    \"email\": {\"type\": \"keyword\"},\n" +
//                "    \"status\": {\"type\": \"keyword\"},\n" +
//                "    \"deleted\": {\"type\": \"keyword\"},\n" +
//                "    \"version\": {\"type\": \"long\"},\n" +
//                "    \"createdAt\": {\"type\": \"date\", \"format\": \"epoch_millis\"},\n" +
//                "    \"updatedAt\": {\"type\": \"date\", \"format\": \"epoch_millis\"},\n" +
//                "    \"canceledAt\": {\"type\": \"date\", \"format\": \"epoch_millis\"}\n" +
//                "  }\n" +
//                "}";
//
//        req.mapping(mapping, XContentType.JSON);
//        es.indices().create(req, RequestOptions.DEFAULT);
//        log.info("ES index created: {}", index);
//    }


    public void initEs() throws Exception {
        String index = props.getEs().getIndex();

        GetIndexRequest get = new GetIndexRequest(index);
        if (es.indices().exists(get, RequestOptions.DEFAULT)) {
            log.info("ES index exists: {}", index);
            return;
        }

        CreateIndexRequest req = new CreateIndexRequest(index);

        // 读取 resources/es/user_profile.json
        try (InputStream is = getClass()
                .getClassLoader()
                .getResourceAsStream("es/user_profile.json")) {

            if (is == null) {
                throw new IllegalStateException("user_profile.json not found in resources/es");
            }

            String source = IOUtils.toString(is, StandardCharsets.UTF_8);
            req.source(source, XContentType.JSON);
        }

        es.indices().create(req, RequestOptions.DEFAULT);
        log.info("ES index created from resource: {}", index);
    }


    private SchemaInitializer getSelf() {
        return SpringUtil.getBean(getClass());
    }
}
