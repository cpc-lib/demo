package cc.ivera.test.base;

import cc.ivera.util.AesUtil;

public class Demo98 {

    // 测试用例
    public static void main(String[] args) {
        try {
            // 生成密钥
            String key256 = AesUtil.generateKey(256);
            System.out.printf("key:%s\n",key256);

            // 测试数据
            String testData1 = "Hello world!";

            // 256-bit测试
            String encrypted256 = AesUtil.encrypt(testData1, key256);
            String decrypted256 = AesUtil.decrypt(encrypted256, key256);


            String data = AesUtil.encrypt(testData1, key256);

            System.out.println(encrypted256);
            System.out.println(data);


            System.out.println("256-bit Test:");
            System.out.println("Key: " + key256);
            System.out.println("Original: " + testData1);
            System.out.println("Encrypted: " + encrypted256);
            System.out.println("Decrypted: " + decrypted256);
            System.out.println("Match: " + testData1.equals(decrypted256));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}