package cc.ivera.util;

import javax.crypto.Cipher;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class XRsaUtil {

    public static final String RSA_ALGORITHM = "RSA";
    public static final String RSA_ALGORITHM_SIGN = "SHA256withRSA";

    /**
     * 固定公钥：仅为了兼容你原来的 setRsaPublicKey(Object json) 调用方式。
     * 生产环境不建议把密钥硬编码在代码中。
     */
    private static final String DEFAULT_PUBLIC_KEY =
            "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAILxLkw91qVRcID_sDpbVSkK1B7mnBpE_eOkU6bK3t-BH7iWgByvsmmwgrIDU2B1m1v7pNJYu4mljHzpDj0XYNECAwEAAQ";

    /**
     * 固定私钥：仅为了兼容你原来的 getData(String json) 调用方式。
     * 生产环境不建议把密钥硬编码在代码中。
     */
    private static final String DEFAULT_PRIVATE_KEY =
            "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAgvEuTD3WpVFwgP-wOltVKQrUHuacGkT946RTpsre34EfuJaAHK-yabCCsgNTYHWbW_uk0li7iaWMfOkOPRdg0QIDAQABAkASlVIBxgDxg2ZZGHCVR6MFaSEDpazf2YzCwu6QTFhnFcMK3z-VXOziZhMw0KYUFMzwQEQu4cKK7olcFFN8poRBAiEAyXi3bI91gkVM3zdbk_8fu5QrEI6kLrj0ydWY6MtHeJkCIQCmYbfCehWMAhFoja3AL5NVovRgNnC4LIISX5gpKTQ0-QIgKxse86VGGRdGuUOY3nNpkLLE_Afo7O45wa1nx_cmVZECIQCRghY2Q5TCfDCDUpyo3jKpCzlTR2ku-OXMccPeA4X_6QIgOl1a5cIxSvXyBnuYGx5ZKS-Yst_BoyMQxMVRgr4bmv4";

    private final RSAPublicKey publicKey;
    private final RSAPrivateKey privateKey;

    public XRsaUtil(String publicKey, String privateKey) {
        this.publicKey = getRSAPublicKey(publicKey);
        this.privateKey = getRSAPrivateKey(privateKey);
    }

    /**
     * 兼容你原来的调用方式：使用固定公钥加密
     */
    public static String setRsaPublicKey(Object json) {
        Objects.requireNonNull(json, "json 不能为空");
        return publicEncrypt(String.valueOf(json), getRSAPublicKey(DEFAULT_PUBLIC_KEY));
    }

    /**
     * 兼容你原来的调用方式：使用固定私钥解密
     */
    public static String getData(String json) {
        Objects.requireNonNull(json, "json 不能为空");
        return privateDecrypt(json, getRSAPrivateKey(DEFAULT_PRIVATE_KEY));
    }

    public static RSAPublicKey getRSAPublicKey(String publicKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(decodeBase64Key(publicKey));
            return (RSAPublicKey) keyFactory.generatePublic(x509KeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("公钥解析失败", e);
        }
    }

    public static RSAPrivateKey getRSAPrivateKey(String privateKey) {
        try {
            KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
            PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(decodeBase64Key(privateKey));
            return (RSAPrivateKey) keyFactory.generatePrivate(pkcs8KeySpec);
        } catch (Exception e) {
            throw new IllegalArgumentException("私钥解析失败", e);
        }
    }

    public static Map<String, String> createKeys(int keySize) {
        if (keySize < 512) {
            throw new IllegalArgumentException("RSA keySize 不能小于 512");
        }

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGORITHM);
            keyPairGenerator.initialize(keySize);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            Key publicKey = keyPair.getPublic();
            Key privateKey = keyPair.getPrivate();

            Map<String, String> keyPairMap = new HashMap<>(2);
            keyPairMap.put("publicKey", Base64.getUrlEncoder().withoutPadding().encodeToString(publicKey.getEncoded()));
            keyPairMap.put("privateKey", Base64.getUrlEncoder().withoutPadding().encodeToString(privateKey.getEncoded()));
            return keyPairMap;
        } catch (Exception e) {
            throw new IllegalStateException("生成 RSA 密钥对失败", e);
        }
    }

    /**
     * 公钥加密
     */
    public String publicEncrypt(String data) {
        ensurePublicKey();
        return publicEncrypt(data, this.publicKey);
    }

    public static String publicEncrypt(String data, RSAPublicKey rsaPublicKey) {
        return publicKeyEncrypt(data, rsaPublicKey);
    }

    private static String publicKeyEncrypt(String data, RSAPublicKey rsaPublicKey) {
        validateText(data, "待加密数据不能为空");
        Objects.requireNonNull(rsaPublicKey, "公钥不能为空");

        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPublicKey);
            byte[] encrypted = rsaSplitCodec(
                    cipher,
                    Cipher.ENCRYPT_MODE,
                    data.getBytes(StandardCharsets.UTF_8),
                    rsaPublicKey.getModulus().bitLength()
            );
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("公钥加密失败", e);
        }
    }

    /**
     * 私钥解密
     */
    public String privateDecrypt(String data) {
        ensurePrivateKey();
        return privateDecrypt(data, this.privateKey);
    }

    public static String privateDecrypt(String data, RSAPrivateKey rsaPrivateKey) {
        validateText(data, "待解密数据不能为空");
        Objects.requireNonNull(rsaPrivateKey, "私钥不能为空");

        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rsaPrivateKey);
            byte[] decrypted = rsaSplitCodec(
                    cipher,
                    Cipher.DECRYPT_MODE,
                    decodeBase64Key(data),
                    rsaPrivateKey.getModulus().bitLength()
            );
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("私钥解密失败", e);
        }
    }

    /**
     * 私钥加密
     */
    public String privateEncrypt(String data) {
        ensurePrivateKey();
        return privateEncrypt(data, this.privateKey);
    }

    public static String privateEncrypt(String data, RSAPrivateKey rsaPrivateKey) {
        validateText(data, "待加密数据不能为空");
        Objects.requireNonNull(rsaPrivateKey, "私钥不能为空");

        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, rsaPrivateKey);
            byte[] encrypted = rsaSplitCodec(
                    cipher,
                    Cipher.ENCRYPT_MODE,
                    data.getBytes(StandardCharsets.UTF_8),
                    rsaPrivateKey.getModulus().bitLength()
            );
            return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted);
        } catch (Exception e) {
            throw new IllegalStateException("私钥加密失败", e);
        }
    }

    /**
     * 公钥解密
     */
    public String publicDecrypt(String data) {
        ensurePublicKey();
        return publicDecrypt(data, this.publicKey);
    }

    public static String publicDecrypt(String data, RSAPublicKey rsaPublicKey) {
        validateText(data, "待解密数据不能为空");
        Objects.requireNonNull(rsaPublicKey, "公钥不能为空");

        try {
            Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, rsaPublicKey);
            byte[] decrypted = rsaSplitCodec(
                    cipher,
                    Cipher.DECRYPT_MODE,
                    decodeBase64Key(data),
                    rsaPublicKey.getModulus().bitLength()
            );
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("公钥解密失败", e);
        }
    }

    /**
     * 私钥签名
     */
    public String sign(String data) {
        ensurePrivateKey();
        return sign(data, this.privateKey);
    }

    public static String sign(String data, RSAPrivateKey privateKey) {
        validateText(data, "待签名数据不能为空");
        Objects.requireNonNull(privateKey, "私钥不能为空");

        try {
            Signature signature = Signature.getInstance(RSA_ALGORITHM_SIGN);
            signature.initSign(privateKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature.sign());
        } catch (Exception e) {
            throw new IllegalStateException("私钥签名失败", e);
        }
    }

    /**
     * 公钥验签
     */
    public boolean verify(String data, String sign) {
        ensurePublicKey();
        return verify(data, sign, this.publicKey);
    }

    public static boolean verify(String data, String sign, RSAPublicKey rsaPublicKey) {
        validateText(data, "原始数据不能为空");
        validateText(sign, "签名不能为空");
        Objects.requireNonNull(rsaPublicKey, "公钥不能为空");

        try {
            Signature signature = Signature.getInstance(RSA_ALGORITHM_SIGN);
            signature.initVerify(rsaPublicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(decodeBase64Key(sign));
        } catch (Exception e) {
            throw new IllegalStateException("公钥验签失败", e);
        }
    }

    /**
     * 分段加解密
     */
    private static byte[] rsaSplitCodec(Cipher cipher, int opmode, byte[] data, int keySize)
            throws GeneralSecurityException {

        int maxBlock;
        if (opmode == Cipher.DECRYPT_MODE) {
            maxBlock = keySize / 8;
        } else {
            maxBlock = keySize / 8 - 11;
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int offset = 0;
            while (offset < data.length) {
                int inputLen = Math.min(data.length - offset, maxBlock);
                byte[] block = cipher.doFinal(data, offset, inputLen);
                out.write(block, 0, block.length);
                offset += inputLen;
            }
            return out.toByteArray();
        } catch (Exception e) {
            throw new GeneralSecurityException("RSA 分段处理失败", e);
        }
    }

    private void ensurePublicKey() {
        if (this.publicKey == null) {
            throw new IllegalStateException("公钥未初始化");
        }
    }

    private void ensurePrivateKey() {
        if (this.privateKey == null) {
            throw new IllegalStateException("私钥未初始化");
        }
    }

    private static void validateText(String text, String message) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * 同时兼容标准 Base64 和 URL Safe Base64
     */
    private static byte[] decodeBase64Key(String text) {
        String normalized = text.replaceAll("\\s+", "");
        int mod = normalized.length() % 4;
        if (mod > 0) {
            normalized = normalized + "=".repeat(4 - mod);
        }

        try {
            return Base64.getUrlDecoder().decode(normalized);
        } catch (IllegalArgumentException ignore) {
            return Base64.getDecoder().decode(normalized);
        }
    }

    public static void main(String[] args) {
        try {
            String publicKey = DEFAULT_PUBLIC_KEY;
            String privateKey = DEFAULT_PRIVATE_KEY;

            RSAPublicKey rsaPublicKey = XRsaUtil.getRSAPublicKey(publicKey);
            RSAPrivateKey rsaPrivateKey = XRsaUtil.getRSAPrivateKey(privateKey);

            String plainText = "{\"name\":\"春江潮水连海平，海上明月共潮生\"}";

            String encrypted = XRsaUtil.publicEncrypt(plainText, rsaPublicKey);
            String decrypted = XRsaUtil.privateDecrypt(encrypted, rsaPrivateKey);

            System.out.println("公钥加密 -> 私钥解密");
            System.out.println("加密结果: " + encrypted);
            System.out.println("解密结果: " + decrypted);

            String sign = XRsaUtil.sign(plainText, rsaPrivateKey);
            boolean verified = XRsaUtil.verify(plainText, sign, rsaPublicKey);

            System.out.println("签名结果: " + sign);
            System.out.println("验签结果: " + verified);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}