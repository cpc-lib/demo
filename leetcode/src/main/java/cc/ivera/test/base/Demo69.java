/**
 * 2023年4月11日上午10:24:39
 */
package cc.ivera.test.base;
 
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
 
/**
 * @author XWF
 *
 */
public class Demo69 {
 
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String pass = "1234567890asdfgh";
		String msg = """
                {"infoList":[{"carplot_id":"1491010","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","plate":"京A12345","carplot_pics":[],"status":1},{"carplot_id":"1491013","carplot_type":1,"equip_id":"PELSL0172","equip_time":"1625488943","carplot_pics":[],"status":"0"},{"carplot_id":"1491014","carplot_type":0,"equip_id":"PELSL0172","equip_time":"1625488943","status":-1}],"park_id":"p001"}
                """;
		try {
			byte[] key = pass.getBytes("utf-8");
			SecretKeySpec keyspec = new SecretKeySpec(key, "AES");
			Cipher cipher = Cipher.getInstance("AES/CBC/ISO10126Padding");	//Modes使用CBC，需要初始化向量IV，填充换个别的试试
			IvParameterSpec iv = new IvParameterSpec("1234567890123456".getBytes("utf-8"));	//初始化向量IV，长度只能16
			cipher.init(Cipher.ENCRYPT_MODE, keyspec, iv);	//CBC模式需要传入初始化向量IV
			byte[] encodeBytes = cipher.doFinal(msg.getBytes("utf-8"));
			System.out.println("使用AES_CBC加密后：" + new String(encodeBytes));
			
			cipher.init(Cipher.DECRYPT_MODE, keyspec, iv);
			byte[] decodeBytes = cipher.doFinal(encodeBytes);
			System.out.println("使用AES_CBC解密后：" + new String(decodeBytes, "utf-8"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
 
}