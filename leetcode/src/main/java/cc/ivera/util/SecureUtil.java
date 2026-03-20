package cc.ivera.util;

import cn.hutool.crypto.symmetric.AES;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class SecureUtil {

    public static final String KEYS = "Q7rgu3Gep4NMJxpk";


    public static final Charset UTF8 = StandardCharsets.UTF_8;


    public static String decrypt(String value) {
        if (null == value) {
            return null;
        }
        return cn.hutool.crypto.SecureUtil.aes(KEYS.getBytes(UTF8)).decryptStr(value);
    }

    public static String encrypt(String value) {
        AES aes = cn.hutool.crypto.SecureUtil.aes(KEYS.getBytes(UTF8));
        String encrypt = aes.encryptHex(value);
        return encrypt;
    }
}
