package cc.ivera.studentdemo.hbase;

import cc.ivera.studentdemo.domain.PageResponse;
import cc.ivera.studentdemo.domain.Student;
import lombok.RequiredArgsConstructor;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.filter.PageFilter;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class StudentRepository {

  private static final TableName TABLE = HBaseTableInitializer.TABLE;
  private static final byte[] CF = HBaseTableInitializer.CF;

  private static byte[] q(String s) { return Bytes.toBytes(s); }

  private final Connection conn;

  private byte[] rk(String id) {
    // demo rowkey: "stu#{id}" so scan is consistent by id order
    return Bytes.toBytes("stu#" + id);
  }

  public boolean exists(String id) throws Exception {
    try (Table t = conn.getTable(TABLE)) {
      Get g = new Get(rk(id)).addColumn(CF, q("id"));
      return t.exists(g);
    }
  }

  public void create(Student s) throws Exception {
    Instant now = Instant.now();
    s.setCreatedAt(now);
    s.setUpdatedAt(now);

    try (Table t = conn.getTable(TABLE)) {
      Put p = new Put(rk(s.getId()))
          .addColumn(CF, q("id"), Bytes.toBytes(s.getId()))
          .addColumn(CF, q("name"), Bytes.toBytes(s.getName()))
          .addColumn(CF, q("age"), Bytes.toBytes(s.getAge()))
          .addColumn(CF, q("grade"), Bytes.toBytes(s.getGrade()))
          .addColumn(CF, q("createdAt"), Bytes.toBytes(s.getCreatedAt().toString()))
          .addColumn(CF, q("updatedAt"), Bytes.toBytes(s.getUpdatedAt().toString()));
      t.put(p);
    }
  }

  public Student get(String id) throws Exception {
    try (Table t = conn.getTable(TABLE)) {
      Get g = new Get(rk(id)).addFamily(CF);
      Result r = t.get(g);
      if (r.isEmpty()) return null;
      return from(r);
    }
  }

  public void update(String id, Student newData) throws Exception {
    Instant now = Instant.now();
    try (Table t = conn.getTable(TABLE)) {
      Put p = new Put(rk(id))
          .addColumn(CF, q("name"), Bytes.toBytes(newData.getName()))
          .addColumn(CF, q("age"), Bytes.toBytes(newData.getAge()))
          .addColumn(CF, q("grade"), Bytes.toBytes(newData.getGrade()))
          .addColumn(CF, q("updatedAt"), Bytes.toBytes(now.toString()));
      t.put(p);
    }
  }

  public void delete(String id) throws Exception {
    try (Table t = conn.getTable(TABLE)) {
      Delete d = new Delete(rk(id));
      t.delete(d);
    }
  }

  public PageResponse<Student> list(int pageSize, String cursor) throws Exception {
    if (pageSize <= 0) pageSize = 10;
    if (pageSize > 200) pageSize = 200;

    List<Student> out = new ArrayList<>(pageSize);
    byte[] lastReturnedRow = null;   // 本页最后一条“实际返回”的 rowkey
    boolean hasMore = false;

    try (Table t = conn.getTable(TABLE)) {
      Scan scan = new Scan().addFamily(CF);
      scan.setCaching(Math.min(100, pageSize + 1));
      scan.setCacheBlocks(false);

      if (StringUtils.hasText(cursor)) {
        byte[] cursorRow = decodeCursor(cursor); // 上一页最后一条返回的 rowkey
        scan.withStartRow(Bytes.add(cursorRow, new byte[]{0x00})); // start AFTER cursor
      } else {
        scan.withStartRow(Bytes.toBytes("stu#"));
      }

      scan.setFilter(new PageFilter(pageSize + 1L));

      try (ResultScanner scanner = t.getScanner(scan)) {
        for (Result r : scanner) {
          Student s = from(r);
          if (s == null) continue;

          if (out.size() < pageSize) {
            out.add(s);
            lastReturnedRow = r.getRow(); // 只记录“实际返回”的最后一条
          } else {
            // 这是第 pageSize+1 条：只用于判断 hasMore，不加入 out
            hasMore = true;
            break;
          }
        }
      }
    }

    String nextCursor = hasMore && lastReturnedRow != null
            ? encodeCursor(lastReturnedRow)  // 下一页从“本页最后返回的 rowkey”之后开始
            : null;

    return PageResponse.<Student>builder()
            .items(out)
            .nextCursor(nextCursor)
            .build();
  }

  /** Base64 URL-safe，无需 URL encode，推荐 */
  private static String encodeCursor(byte[] row) {
    return Base64.getUrlEncoder().withoutPadding().encodeToString(row);
  }

  private static byte[] decodeCursor(String cursor) {
    return Base64.getUrlDecoder().decode(cursor);
  }

  private Student from(Result r) {
    String id = s(r.getValue(CF, q("id")));
    if (id == null) {
      // fallback from rowkey
      String row = Bytes.toString(r.getRow());
      if (row != null && row.startsWith("stu#")) id = row.substring(4);
    }
    Student st = new Student();
    st.setId(id);
    st.setName(s(r.getValue(CF, q("name"))));
    byte[] ageB = r.getValue(CF, q("age"));
    st.setAge(ageB == null ? null : Bytes.toInt(ageB));
    st.setGrade(s(r.getValue(CF, q("grade"))));
    st.setCreatedAt(parseInstant(s(r.getValue(CF, q("createdAt")))));
    st.setUpdatedAt(parseInstant(s(r.getValue(CF, q("updatedAt")))));
    return st;
  }

  private static String s(byte[] b) { return b == null ? null : Bytes.toString(b); }

  private static Instant parseInstant(String s) {
    if (!StringUtils.hasText(s)) return null;
    try { return Instant.parse(s); } catch (Exception e) { return null; }
  }
}
