package top.arhi.wxpush.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Service;
import top.arhi.wxpush.constant.ConfigConstant;
import top.arhi.wxpush.service.ProverbService;
import top.arhi.wxpush.utils.AuthV3Util;
import top.arhi.wxpush.utils.HttpUtil;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;


@Service
public class ProverbServiceImpl implements ProverbService {
    @Resource
    private ConfigConstant configConstant;

    /**
     * 获取一个随机的谚语
     *
     * @return
     */
    @Override
    public String getOneProverbRandom() {
        String proverb;
        do {
            proverb = null;
            try {
                OkHttpClient client = new OkHttpClient().newBuilder().build();
                Request request = new Request.Builder()
                        .url("https://api.xygeng.cn/one")
                        .get()
                        .addHeader("Content-Type", "")
                        .build();
                Response response = client.newCall(request).execute();
                //解析
                JSONObject jsonObject = JSONObject.parseObject(response.body().string());
                JSONObject content = jsonObject.getJSONObject("data");
                proverb = content.getString("content");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } while (proverb.length() > 25);
        return proverb;
    }

    private static Map<String, String[]> createRequestParams(String q) {
        /*
         * note: 将下列变量替换为需要请求的参数
         * 取值参考文档: https://ai.youdao.com/DOCSIRMA/html/%E8%87%AA%E7%84%B6%E8%AF%AD%E8%A8%80%E7%BF%BB%E8%AF%91/API%E6%96%87%E6%A1%A3/%E6%96%87%E6%9C%AC%E7%BF%BB%E8%AF%91%E6%9C%8D%E5%8A%A1/%E6%96%87%E6%9C%AC%E7%BF%BB%E8%AF%91%E6%9C%8D%E5%8A%A1-API%E6%96%87%E6%A1%A3.html
         */
        String from = "zh-CHS";
        String to = "en";
//        String vocabId = "您的用户词表ID";

        return new HashMap<String, String[]>() {{
            put("q", new String[]{q});
            put("from", new String[]{from});
            put("to", new String[]{to});
//            put("vocabId", new String[]{vocabId});
        }};
    }

    /**
     * 翻译中文为英文
     *
     * @param sentence
     * @return
     */
    @Override
    public String translateToEnglish(String sentence) {
        String result = null;
        try {
            // 添加请求参数
            Map<String, String[]> params = createRequestParams(sentence);
            // 添加鉴权相关参数
            AuthV3Util.addAuthParams(configConstant.getAppKey_YouDao(), configConstant.getAppSecret_YouDao(), params);
            // 请求api服务
            byte[] resultb = HttpUtil.doPost("https://openapi.youdao.com/api", null, params, "application/json");
            result = new String(resultb, StandardCharsets.UTF_8);
            result = JSONObject.parseObject(result).getJSONArray("translation").get(0).toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /**
     * APISpace的获取谚语的接口
     *
     * @return
     */
    @Override
    public String getOneNormalProverb() {
        String proverb = null;
        try {
            OkHttpClient client = new OkHttpClient().newBuilder().build();
            MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
            RequestBody body = RequestBody.create(mediaType, "titleID=" + new Random().nextInt(9));
            Request request = new Request.Builder()
                    .url("https://eolink.o.apispace.com/myjj/common/aphorism/getAphorismList")
                    .method("POST", body)
                    .addHeader("X-APISpace-Token", configConstant.getToken())
                    .addHeader("Authorization-Type", "apikey")
                    .addHeader("Content-Type", "")
                    .build();

            Response response = client.newCall(request).execute();
            JSONObject jsonObject = JSONObject.parseObject(response.body().string());
            //取出全部句子
            JSONArray allProverb = JSONObject.parseArray((String) jsonObject.getJSONArray("result").getJSONObject(0).get("words"));
            // 新增 微信限制只能20个字符，因此新增限制，2023-08-22更新
            List<Object> collect = allProverb.stream().filter(o ->
                    ((String) o).length() <= 20
            ).collect(Collectors.toList());
            //随机取出一条句子
            String s = (String) collect.get(new Random().nextInt(collect.size()));
            //去除无关元素
            proverb = s.replaceAll("^.*、", "");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return proverb;
    }


}
