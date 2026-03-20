package cc.ivera.test.base;

import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.engines.RijndaelEngine;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;

import java.util.Base64;

/**
 * 数据加密传递方式
 */
public class Demo68 {
    public String encrypt(String key, String content) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new RijndaelEngine(256),
                new PKCS7Padding()
        );
        CipherParameters parameters = new KeyParameter(key.getBytes());
        cipher.init(true, parameters);

        byte[] txt = content.getBytes();
        byte[] encoded = new byte[cipher.getOutputSize(txt.length)];
        int len = cipher.processBytes(txt, 0, txt.length, encoded, 0);
        cipher.doFinal(encoded, len);
        String encString = Base64.getEncoder().encodeToString(encoded);
        return encString;
    }

    public String decrypt(String key, String encrypted) throws InvalidCipherTextException {
        PaddedBufferedBlockCipher cipher = new PaddedBufferedBlockCipher(
                new RijndaelEngine(256),
                new PKCS7Padding()
        );
        CipherParameters parameters = new KeyParameter(key.getBytes());
        cipher.init(false, parameters);

        byte[] encoded = Base64.getDecoder().decode(encrypted.trim());
        byte[] decoded = new byte[cipher.getOutputSize(encoded.length)];
        int len1 = cipher.processBytes(encoded, 0, encoded.length, decoded, 0);
        int len2 = cipher.doFinal(decoded, len1);

        int actualLength = len1 + len2;
        byte[] cipherArray = new byte[actualLength];
        for (int i = 0; i < actualLength; i++) {
            cipherArray[i] = decoded[i];
        }
        String decString = new String(cipherArray);
        return decString;
    }


    public static void main(String[] args) throws InvalidCipherTextException {

        String key = "gJAeI90qJjpxQYS2GcwontjKEf89QTvJ";
        String content = """
                {"park_orderid":"osj812jasd","plate":"京A29S0","mobile":"15701208329","park_id":"2307c94ca527bddb","reserve_time":"1625128223","arrival_time":"1625128323","leave_time":"1625148323","order_status":1,"reserve_fee":"","reserve_total_fee":"","reserve_deduct_point":"","reserve_return_point":""}
                """;
        String encryedString = "";
        String decrypedString = "";
        Demo68 aes256Demo = new Demo68();
        try {
            encryedString = aes256Demo.encrypt(key, content);
            System.out.println(encryedString);
            decrypedString = aes256Demo.decrypt(key, encryedString);
            System.out.println(decrypedString);
        } catch (InvalidCipherTextException e) {
            e.printStackTrace();
        }

        String decrypt = aes256Demo.decrypt(key, encryedString);
        if (decrypt.equals(content)) {
            System.out.println(true);
        }

    }


}