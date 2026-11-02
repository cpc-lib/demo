package cc.ivera.test.base;


import cc.ivera.util.AuthV3Util;
import cc.ivera.util.HttpUtil;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

/**
 * 网易有道智云翻译服务api调用demo
 * api接口: https://openapi.youdao.com/api
 */
public class Demo52 {

    // 从环境变量读取，禁止在代码中写死应用ID与密钥
    private static final String APP_KEY = getRequiredEnv("YOUDAO_APP_KEY");
    private static final String APP_SECRET = getRequiredEnv("YOUDAO_APP_SECRET");

    private static String getRequiredEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("请先配置环境变量: " + name);
        }
        return value;
    }

    public static void main(String[] args) throws NoSuchAlgorithmException {
        // 添加请求参数
        Map<String, String[]> params = createRequestParams();
        // 添加鉴权相关参数
        AuthV3Util.addAuthParams(APP_KEY, APP_SECRET, params);
        // 请求api服务
        byte[] result = HttpUtil.doPost("https://openapi.youdao.com/api", null, params, "application/json");
        // 打印返回结果
        if (result != null) {
            System.out.println(new String(result, StandardCharsets.UTF_8));
        }
        System.exit(0);
    }

    private static Map<String, String[]> createRequestParams() {
        /*
         * note: 将下列变量替换为需要请求的参数
         * 取值参考文档: https://ai.youdao.com/DOCSIRMA/html/trans/api/wbfy/index.html
         */
        String q = "Hello";
        String from = "auto";
        String to = "zh-CHS";
        //用户指定的词典 out_id，支持英中互译，更多语种方向请前往控制台查询
        String vocabId = "您的用户词表ID";

        return new HashMap<String, String[]>() {{
            put("q", new String[]{q});
            put("from", new String[]{from});
            put("to", new String[]{to});
            put("vocabId", new String[]{vocabId});
        }};
    }
}
