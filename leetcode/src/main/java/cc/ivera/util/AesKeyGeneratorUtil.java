package cc.ivera.util;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

public class AesKeyGeneratorUtil {
    public static byte[] generateKey(int keyLength) {
        validateKeyLength(keyLength);

        try {
            KeyGenerator keyGen = KeyGenerator.getInstance("AES");
            SecureRandom secureRandom = SecureRandom.getInstanceStrong();
            keyGen.init(keyLength * 8, secureRandom);
            SecretKey secretKey = keyGen.generateKey();
            return secretKey.getEncoded();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("AES algorithm not available", e);
        }
    }

    public static String generateBase64Key(int keyLength) {
        return Base64.getEncoder().encodeToString(generateKey(keyLength));
    }

    public static String generateHexKey(int keyLength) {
        return bytesToHex(generateKey(keyLength));
    }

    private static void validateKeyLength(int keyLength) {
        if (keyLength != 16 && keyLength != 24 && keyLength != 32) {
            throw new IllegalArgumentException(
                    "Invalid AES key length: " + keyLength +
                            ". Must be 16 (128bit), 24 (192bit) or 32 (256bit)"
            );
        }
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
