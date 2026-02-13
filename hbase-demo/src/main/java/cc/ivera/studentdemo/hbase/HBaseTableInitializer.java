package cc.ivera.studentdemo.hbase;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.NamespaceDescriptor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Admin;
import org.apache.hadoop.hbase.client.ColumnFamilyDescriptorBuilder;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.TableDescriptorBuilder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class HBaseTableInitializer {

  public static final String NAMESPACE = "demo";
  public static final TableName TABLE = TableName.valueOf(NAMESPACE + ":student");
  public static final byte[] CF = org.apache.hadoop.hbase.util.Bytes.toBytes("cf");

  private final Connection conn;

  public void ensureTable() throws Exception {
    try (Admin admin = conn.getAdmin()) {
      // namespace
      boolean nsExists;
      try {
        admin.getNamespaceDescriptor(NAMESPACE);
        nsExists = true;
      } catch (Exception e) {
        nsExists = false;
      }
      if (!nsExists) {
        admin.createNamespace(NamespaceDescriptor.create(NAMESPACE).build());
        log.info("Created namespace {}", NAMESPACE);
      }

      if (!admin.tableExists(TABLE)) {
        var cfd = ColumnFamilyDescriptorBuilder.newBuilder(CF)
            .setMaxVersions(1)
            .build();
        var td = TableDescriptorBuilder.newBuilder(TABLE)
            .setColumnFamily(cfd)
            .build();
        admin.createTable(td);
        log.info("Created table {}", TABLE.getNameAsString());
      }
    }
  }
}
