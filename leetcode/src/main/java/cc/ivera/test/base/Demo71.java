/**
 * 2023年4月11日上午11:09:02
 */
package cc.ivera.test.base;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author XWF
 */
public class Demo71 {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String pass = "1122334455667788";
        String msg = """
                {"infoList":[{"carplot_id":"1491010","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","plate":"京A12345","carplot_pics":[],"status":1},{"carplot_id":"1491013","carplot_type":1,"equip_id":"PELSL0172","equip_time":"1625488943","carplot_pics":[],"status":"0"},{"carplot_id":"1491014","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","status":-1}],"park_id":"p001"}
                """;
        try {
            byte[] key = pass.getBytes("utf-8");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
            //加密
            Cipher cipher = Cipher.getInstance("AES/OFB24/PKCS5Padding");
            IvParameterSpec iv = new IvParameterSpec("1234567887654321".getBytes("utf-8"));
            cipher.init(Cipher.ENCRYPT_MODE, keyspec, iv);
            byte[] encodeBytes = cipher.doFinal(msg.getBytes("utf-8"));
            System.out.println("使用AES_OFB加密后：" + new String(encodeBytes));

            //解密
            cipher.init(Cipher.DECRYPT_MODE, keyspec, iv);
            byte[] decodeBytes = cipher.doFinal(encodeBytes);
            System.out.println("使用AES_OFB解密后：" + new String(decodeBytes, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}