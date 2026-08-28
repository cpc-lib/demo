package cc.ivera.userdemo.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Shards {

  private final int tableCount;

  public Shards(@Value("${app.shard.table-count:4}") int tableCount) {
    this.tableCount = tableCount;
  }

  public int tableCount() { return tableCount; }

  public String suffixByLong(long v) {
    int mod = (int) (Math.floorMod(v, tableCount));
    return String.format("%02d", mod);
  }

  public String suffixByString(String s) {
    int h = (s == null ? 0 : s.hashCode());
    int mod = Math.floorMod(h, tableCount);
    return String.format("%02d", mod);
  }

  public String userBaseTable(long uid) {
    return "user_base_" + suffixByLong(uid);
  }

  public String phoneIndexTable(String phone) {
    return "user_phone_index_" + suffixByString(phone);
  }

  public String emailIndexTable(String emailLower) {
    return "user_email_index_" + suffixByString(emailLower);
  }
}
