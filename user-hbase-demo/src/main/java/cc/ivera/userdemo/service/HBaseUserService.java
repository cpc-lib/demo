package cc.ivera.userdemo.service;

import cc.ivera.userdemo.domain.UserProfile;
import cc.ivera.userdemo.util.Salting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.*;
import org.apache.hadoop.hbase.util.Bytes;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HBaseUserService {

  private final Connection conn;

  private static final byte[] CF = Bytes.toBytes("cf");
  private static byte[] q(String s) { return Bytes.toBytes(s); }
  private static byte[] b(String s) { return s == null ? null : Bytes.toBytes(s); }

  public boolean acquirePhone(String phone, String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_phone"))) {
      byte[] row = Bytes.toBytes(Salting.rk(phone));
      Put put = new Put(row).addColumn(CF, q("uid"), Bytes.toBytes(uid));
      CheckAndMutate cam = CheckAndMutate.newBuilder(row).ifNotExists(CF, q("uid")).build(put);
      return t.checkAndMutate(cam).isSuccess();
    }
  }

  public boolean acquireEmail(String emailLower, String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_email"))) {
      byte[] row = Bytes.toBytes(Salting.rk(emailLower));
      Put put = new Put(row).addColumn(CF, q("uid"), Bytes.toBytes(uid));
      CheckAndMutate cam = CheckAndMutate.newBuilder(row).ifNotExists(CF, q("uid")).build(put);
      return t.checkAndMutate(cam).isSuccess();
    }
  }

  public boolean releasePhone(String phone, String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_phone"))) {
      byte[] row = Bytes.toBytes(Salting.rk(phone));
      Delete del = new Delete(row);
      CheckAndMutate cam = CheckAndMutate.newBuilder(row).ifEquals(CF, q("uid"), Bytes.toBytes(uid)).build(del);
      return t.checkAndMutate(cam).isSuccess();
    }
  }

  public boolean releaseEmail(String emailLower, String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_email"))) {
      byte[] row = Bytes.toBytes(Salting.rk(emailLower));
      Delete del = new Delete(row);
      CheckAndMutate cam = CheckAndMutate.newBuilder(row).ifEquals(CF, q("uid"), Bytes.toBytes(uid)).build(del);
      return t.checkAndMutate(cam).isSuccess();
    }
  }

  public void putUser(String uid, String phone, String emailLower, String pwdHash, long version,
                      String status, String deleted, long createdAt, long updatedAt, Long canceledAt) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:user"))) {
      Put p = new Put(Bytes.toBytes(Salting.rk(uid)))
          .addColumn(CF, q("uid"), Bytes.toBytes(uid))
          .addColumn(CF, q("phone"), b(phone))
          .addColumn(CF, q("email"), b(emailLower))
          .addColumn(CF, q("pwd_hash"), b(pwdHash))
          .addColumn(CF, q("status"), b(status))
          .addColumn(CF, q("deleted"), b(deleted))
          .addColumn(CF, q("version"), Bytes.toBytes(version))
          .addColumn(CF, q("created_at"), Bytes.toBytes(createdAt))
          .addColumn(CF, q("updated_at"), Bytes.toBytes(updatedAt));
      if (canceledAt != null) {
        p.addColumn(CF, q("canceled_at"), Bytes.toBytes(canceledAt));
      }
      t.put(p);
    }
  }

  public Optional<UserProfile> getUser(String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:user"))) {
      Get g = new Get(Bytes.toBytes(Salting.rk(uid))).addFamily(CF);
      Result r = t.get(g);
      if (r.isEmpty()) return Optional.empty();

      String phone = str(r.getValue(CF, q("phone")));
      String email = str(r.getValue(CF, q("email")));
      String status = str(r.getValue(CF, q("status")));
      String deleted = str(r.getValue(CF, q("deleted")));
      long version = longVal(r.getValue(CF, q("version")));
      long createdAt = longVal(r.getValue(CF, q("created_at")));
      long updatedAt = longVal(r.getValue(CF, q("updated_at")));
      Long canceledAt = optLong(r.getValue(CF, q("canceled_at")));

      return Optional.of(UserProfile.builder()
          .uid(uid).phone(phone).email(email).status(status).deleted(deleted)
          .version(version).createdAt(createdAt).updatedAt(updatedAt).canceledAt(canceledAt)
          .build());
    }
  }

  public Optional<String> getPwdHash(String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:user"))) {
      Get g = new Get(Bytes.toBytes(Salting.rk(uid))).addColumn(CF, q("pwd_hash"));
      Result r = t.get(g);
      if (r.isEmpty()) return Optional.empty();
      return Optional.ofNullable(str(r.getValue(CF, q("pwd_hash"))));
    }
  }

  public Optional<String> uidByPhone(String phone) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_phone"))) {
      Result r = t.get(new Get(Bytes.toBytes(Salting.rk(phone))).addColumn(CF, q("uid")));
      if (r.isEmpty()) return Optional.empty();
      return Optional.ofNullable(str(r.getValue(CF, q("uid"))));
    }
  }

  public Optional<String> uidByEmail(String emailLower) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:uniq_email"))) {
      Result r = t.get(new Get(Bytes.toBytes(Salting.rk(emailLower))).addColumn(CF, q("uid")));
      if (r.isEmpty()) return Optional.empty();
      return Optional.ofNullable(str(r.getValue(CF, q("uid"))));
    }
  }

  public long bumpVersionAndMarkCanceled(String uid) throws IOException {
    // 简化：读 version 再写 version+1；生产可用 checkAndMutate 做 CAS（避免并发写丢）
    UserProfile u = getUser(uid).orElseThrow(() -> new IllegalArgumentException("user not found"));
    long newVer = u.getVersion() + 1;
    long now = System.currentTimeMillis();
    putUser(uid, u.getPhone(), u.getEmail(), getPwdHash(uid).orElse(""),
        newVer, "CANCELED", "1", u.getCreatedAt(), now, now);
    return newVer;
  }

  public long bumpVersionAndUpdateContact(String uid, String newPhone, String newEmailLower) throws IOException {
    UserProfile u = getUser(uid).orElseThrow(() -> new IllegalArgumentException("user not found"));
    long newVer = u.getVersion() + 1;
    long now = System.currentTimeMillis();
    String pwd = getPwdHash(uid).orElse("");
    putUser(uid, newPhone, newEmailLower, pwd, newVer, u.getStatus(), u.getDeleted(), u.getCreatedAt(), now, u.getCanceledAt());
    return newVer;
  }

  public static String bcrypt(String raw) { return BCrypt.hashpw(raw, BCrypt.gensalt(10)); }
  public static boolean bcryptMatches(String raw, String hash) { return BCrypt.checkpw(raw, hash); }

  public Optional<UserProfile> getProfile(String uid) throws IOException {
    try (Table t = conn.getTable(TableName.valueOf("u:user"))) {
      Get g = new Get(Bytes.toBytes(Salting.rk(uid)))
          .addColumn(CF, q("uid"))
          .addColumn(CF, q("phone"))
          .addColumn(CF, q("email"))
          .addColumn(CF, q("status"))
          .addColumn(CF, q("deleted"))
          .addColumn(CF, q("version"))
          .addColumn(CF, q("created_at"))
          .addColumn(CF, q("updated_at"))
          .addColumn(CF, q("canceled_at"));
      Result r = t.get(g);
      if (r.isEmpty()) return Optional.empty();

      return Optional.of(UserProfile.builder()
          .uid(uid)
          .phone(str(r.getValue(CF, q("phone"))))
          .email(str(r.getValue(CF, q("email"))))
          .status(str(r.getValue(CF, q("status"))))
          .deleted(str(r.getValue(CF, q("deleted"))))
          .version(longVal(r.getValue(CF, q("version"))))
          .createdAt(longVal(r.getValue(CF, q("created_at"))))
          .updatedAt(longVal(r.getValue(CF, q("updated_at"))))
          .canceledAt(optLong(r.getValue(CF, q("canceled_at"))))
          .build());
    }
  }

  private static String str(byte[] v) { return v == null ? null : Bytes.toString(v); }
  private static long longVal(byte[] v) { return v == null ? 0L : Bytes.toLong(v); }
  private static Long optLong(byte[] v) { return v == null ? null : Bytes.toLong(v); }
}