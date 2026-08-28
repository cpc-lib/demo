package cc.ivera.ragdemo.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * API Key 加密/解密工具类
 * 使用 AES-GCM 算法对 API Key 进行可逆加密存储
 *
 * 加密格式：Base64(iv || ciphertext+tag)
 * - iv: 12字节随机初始化向量
 * - ciphertext+tag: AES-GCM 加密后的密文+认证标签
 */
public final class ApiKeyEncryptor {

    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;

    /** 加密前缀，用于判断是否已加密 */
    private static final String ENCRYPTED_PREFIX = "ENC:";

    private final SecretKeySpec secretKey;

    /**
     * @param encryptionKey 加密密钥（从配置读取），至少16字符
     */
    public ApiKeyEncryptor(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.length() < 16) {
            throw new IllegalArgumentException("Encryption key must be at least 16 characters");
        }
        try {
            // 使用 SHA-256 派生固定长度的 AES 密钥
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(encryptionKey.getBytes(StandardCharsets.UTF_8));
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize ApiKeyEncryptor", e);
        }
    }

    /**
     * 加密 API Key
     *
     * @param plainText 明文 API Key
     * @return 加密后的字符串（格式：ENC:Base64(iv+ciphertext)）
     */
    public String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return plainText;
        }
        // 已加密的不重复加密
        if (isEncrypted(plainText)) {
            return plainText;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            SecureRandom random = new SecureRandom();
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            // 合并 iv 和 cipherText
            byte[] combined = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(cipherText, 0, combined, iv.length, cipherText.length);

            return ENCRYPTED_PREFIX + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt API key", e);
        }
    }

    /**
     * 解密 API Key
     *
     * @param encryptedText 加密的 API Key（格式：ENC:Base64(iv+ciphertext)）
     * @return 明文 API Key
     */
    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isEmpty()) {
            return encryptedText;
        }
        // 非加密格式直接返回（兼容旧数据）
        if (!isEncrypted(encryptedText)) {
            return encryptedText;
        }
        try {
            String base64Part = encryptedText.substring(ENCRYPTED_PREFIX.length());
            byte[] combined = Base64.getDecoder().decode(base64Part);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            byte[] cipherText = new byte[combined.length - IV_LENGTH_BYTES];
            System.arraycopy(combined, 0, iv, 0, IV_LENGTH_BYTES);
            System.arraycopy(combined, IV_LENGTH_BYTES, cipherText, 0, cipherText.length);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plainText = cipher.doFinal(cipherText);
            return new String(plainText, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt API key", e);
        }
    }

    /**
     * 判断字符串是否已加密
     */
    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }
}
