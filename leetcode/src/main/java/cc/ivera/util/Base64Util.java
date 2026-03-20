package cc.ivera.util;

import org.springframework.util.Base64Utils;

public class Base64Util {

    /***
     * BASE64解密
     * @param key
     * @return
     * @throws Exception
     */
    public static byte[] decode(String key) throws Exception {
        return Base64Utils.decodeFromString(key);
    }

    /***
     * BASE64加密
     * @param bytes
     * @return
     * @throws Exception
     */
    public static String encode(byte[] bytes) throws Exception {
        return Base64Utils.encodeToString(bytes);
    }


}
