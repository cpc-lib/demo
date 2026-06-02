package cc.ivera.util;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

public class AESEncryptionToolkit {

    // AES加密解密工具类
    public static class AESUtil {

        public static String encrypt(String content, byte[] key) throws Exception {
            validateKeyLength(key);

            byte[] iv = new byte[16];
            new SecureRandom().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

            byte[] encrypted = cipher.doFinal(content.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);

            return Base64.getEncoder().encodeToString(combined);
        }

        public static String decrypt(String encryptedContent, byte[] key) throws Exception {
            validateKeyLength(key);

            byte[] combined = Base64.getDecoder().decode(encryptedContent);
            byte[] iv = new byte[16];
            byte[] encrypted = new byte[combined.length - iv.length];

            System.arraycopy(combined, 0, iv, 0, iv.length);
            System.arraycopy(combined, iv.length, encrypted, 0, encrypted.length);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted, StandardCharsets.UTF_8);
        }

        private static void validateKeyLength(byte[] key) {
            if (key.length != 16 && key.length != 24 && key.length != 32) {
                throw new IllegalArgumentException("Invalid AES key length (16/24/32 bytes)");
            }
        }
    }

    // 集成测试用例
    public static void main(String[] args) {
        try {
            // 生成测试密钥
            byte[] key128 = AesKeyGeneratorUtil.generateKey(16);
            byte[] key256 = AesKeyGeneratorUtil.generateKey(32);

            // 测试数据
            String testData = "AES Integration Test: 128/256-bit Encryption";

            // 128-bit加密解密测试
            String encrypted128 = AESUtil.encrypt(testData, key128);
            String decrypted128 = AESUtil.decrypt(encrypted128, key128);
            System.out.println("128-bit Test:");
            System.out.println("Original: " + testData);
            System.out.println("Encrypted: " + encrypted128.substring(0, 32) + "...");
            System.out.println("Decrypted: " + decrypted128);
            System.out.println("Match: " + testData.equals(decrypted128) + "\n");

            // 256-bit加密解密测试
            String encrypted256 = AESUtil.encrypt(testData, key256);
            String decrypted256 = AESUtil.decrypt(encrypted256, key256);
            System.out.println("256-bit Test:");
            System.out.println("Original: " + testData);
            System.out.println("Encrypted: " + encrypted256.substring(0, 32) + "...");
            System.out.println("Decrypted: " + decrypted256);
            System.out.println("Match: " + testData.equals(decrypted256) + "\n");

            // 显示生成的密钥
            System.out.println("Generated Keys:");
            System.out.println("128-bit (Hex): " + AesKeyGeneratorUtil.bytesToHex(key128));
            System.out.println("256-bit (Base64): " + AesKeyGeneratorUtil.generateBase64Key(32));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}