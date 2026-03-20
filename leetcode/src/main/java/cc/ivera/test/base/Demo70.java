/**
 * 2023年4月11日上午10:15:23
 */
package cc.ivera.test.base;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

/**
 * @author XWF
 */
public class Demo70 {

    /**
     * @param args
     */
    public static void main(String[] args) {
        String pass = "1234567890asdfgh";    //加密解密用的密码，长度16或者24或者32
        String msg = """
                {"infoList":[{"carplot_id":"1491010","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","plate":"京A12345","carplot_pics":[],"status":1},{"carplot_id":"1491013","carplot_type":1,"equip_id":"PELSL0172","equip_time":"1625488943","carplot_pics":[],"status":"0"},{"carplot_id":"1491014","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","status":-1}],"park_id":"p001"}
                """;
        try {
            byte[] key = pass.getBytes("utf-8");
            SecretKeySpec keyspec = new SecretKeySpec(key, "AES");    //使用AES

            //加密
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");    //参数格式：Names/Modes/Padding，可以组合出多种
            cipher.init(Cipher.ENCRYPT_MODE, keyspec);    //加密模式
            byte[] encodeBytes = cipher.doFinal(msg.getBytes("utf-8"));
            System.out.println("AES_ECB加密后：" + new String(encodeBytes));

            //解密
            cipher.init(Cipher.DECRYPT_MODE, keyspec);    //解密模式（对称加密使用相同密码）
            byte[] decodeBytes = cipher.doFinal(encodeBytes);
            System.out.println("AES_ECB解密后：" + new String(decodeBytes, "utf-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}