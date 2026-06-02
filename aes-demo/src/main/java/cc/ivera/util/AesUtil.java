package cc.ivera.util;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesUtil {

    /**
     * 使用字符串密钥加密
     * @param content 明文内容
     * @param base64Key Base64编码的密钥字符串
     * @return Base64编码的加密结果
     */
    public static String encrypt(String content, String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        validateKeyLength(keyBytes);

        // 生成随机IV
        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);

        // 初始化加密器
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

        // 执行加密
        byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

        // 合并IV和密文
        byte[] combined = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    /**
     * 使用字符串密钥解密
     * @param encryptedContent Base64编码的加密内容
     * @param base64Key Base64编码的密钥字符串
     * @return 解密后的明文
     */
    public static String decrypt(String encryptedContent, String base64Key) throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(base64Key);
        validateKeyLength(keyBytes);

        // Base64解码
        byte[] combined = Base64.getDecoder().decode(encryptedContent);

        // 提取IV和密文
        byte[] iv = new byte[16];
        byte[] encrypted = new byte[combined.length - iv.length];
        System.arraycopy(combined, 0, iv, 0, iv.length);
        System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

        // 初始化解密器
        SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

        // 执行解密
        byte[] decrypted = cipher.doFinal(encrypted);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    private static void validateKeyLength(byte[] keyBytes) {
        int keyLength = keyBytes.length * 8;
        if (keyLength != 128 && keyLength != 192 && keyLength != 256) {
            throw new IllegalArgumentException(
                    "Invalid AES key length: " + keyLength + " bits. " +
                            "Must be 128/192/256 bits (16/24/32 bytes)"
            );
        }
    }

    /**
     * 生成AES密钥字符串（Base64编码）
     * @param keyLength 密钥长度：128/192/256（单位：bit）
     * @return Base64编码的密钥字符串
     */
    public static String generateKey(int keyLength) {
        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            keyGen.init(keyLength, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return Base64.getEncoder().encodeToString(secretKey.getEncoded());
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES algorithm not available", e);
        }
    }
}
