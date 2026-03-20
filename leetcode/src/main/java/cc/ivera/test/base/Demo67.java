package cc.ivera.test.base;

import cc.ivera.util.XRsaUtil;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

public class Demo67 {

    public static void main(String[] args) {
        //这是个单个用例，对于微信大公司而言,个人觉得这个东西应该是需要进行存储到数据进行处理
        //敏感数据存储到数据库里面如手机号码,身份证信息存储到数据库中需要加密处理的
        //适应业务给自己留下发展空间
        try {
            String publicKey = "MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBAILxLkw91qVRcID_sDpbVSkK1B7mnBpE_eOkU6bK3t-BH7iWgByvsmmwgrIDU2B1m1v7pNJYu4mljHzpDj0XYNECAwEAAQ";
            String privateKey = "MIIBVAIBADANBgkqhkiG9w0BAQEFAASCAT4wggE6AgEAAkEAgvEuTD3WpVFwgP-wOltVKQrUHuacGkT946RTpsre34EfuJaAHK-yabCCsgNTYHWbW_uk0li7iaWMfOkOPRdg0QIDAQABAkASlVIBxgDxg2ZZGHCVR6MFaSEDpazf2YzCwu6QTFhnFcMK3z-VXOziZhMw0KYUFMzwQEQu4cKK7olcFFN8poRBAiEAyXi3bI91gkVM3zdbk_8fu5QrEI6kLrj0ydWY6MtHeJkCIQCmYbfCehWMAhFoja3AL5NVovRgNnC4LIISX5gpKTQ0-QIgKxse86VGGRdGuUOY3nNpkLLE_Afo7O45wa1nx_cmVZECIQCRghY2Q5TCfDCDUpyo3jKpCzlTR2ku-OXMccPeA4X_6QIgOl1a5cIxSvXyBnuYGx5ZKS-Yst_BoyMQxMVRgr4bmv4";

            RSAPublicKey rsaPublicKey = XRsaUtil.getRSAPublicKey(publicKey);
            RSAPrivateKey rsaPrivateKey = XRsaUtil.getRSAPrivateKey(privateKey);

            String json = "{\"name\" : \"床前明月光，大北有点慌\"}";

            // 私钥加密得到sign
            String sign = XRsaUtil.sign(json, rsaPrivateKey);
            System.out.println("sign:" + sign);
            boolean b = XRsaUtil.verifySign(json, sign, rsaPublicKey);
            System.out.println("验证签名:" + b);

            String en = XRsaUtil.publicEncrypt(json, rsaPublicKey);
            String de = XRsaUtil.privateDecrypt(en, rsaPrivateKey);

            System.out.println("公钥(public)加密,私钥(private)解密---------");
            System.out.println("公钥加密json数据:" + en);
            System.out.println("私钥解密:" + de);

            en = XRsaUtil.privateEncrypt(json, rsaPrivateKey);
            de = XRsaUtil.publicDecrypt(en, rsaPublicKey);

            System.out.println("私钥(private)加密，公钥(public)解密---------");
            System.out.println("私钥加密json数据:" + en);
            System.out.println("公钥解密:" + de);

            System.out.println("--------------------------------------------------------------------------------");


        } catch (Exception e) {
            System.out.println("Exception thrown: " + e);
        }

    }
}