package cc.ivera.userdemo.util;

public class Salting {
  private Salting() {}

  public static String saltHex(String s) {
    int h = (s == null ? 0 : s.hashCode());
    int b = (h & 0xff);
    return String.format("%02x", b);
  }

  public static String rk(String key) {
    return saltHex(key) + "_" + key;
  }
}
