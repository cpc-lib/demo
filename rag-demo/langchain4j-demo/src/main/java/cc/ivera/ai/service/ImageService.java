package cc.ivera.ai.service;

import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.utils.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ImageService {

    private final String apiKey;

    public ImageService(@Value("${ai.dashscope.api-key}") String apiKey) {
        this.apiKey = apiKey;
        // 国内地域（北京）；如需新加坡/海外，按官方文档替换 endpoint
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }

    public String generateImageUrl(String prompt) {
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("prompt_extend", true);
        parameters.put("watermark", false);
        parameters.put("negative_prompt", " ");

        ImageSynthesisParam param = ImageSynthesisParam.builder()
                .apiKey(apiKey)
                .model("qwen-image-plus")
                .prompt(prompt)
                .n(1)
                .size("1024*1024")
                .parameters(parameters)
                .build();

        try {
            ImageSynthesisResult result = new ImageSynthesis().call(param);

            if (result == null
                    || result.getOutput() == null
                    || result.getOutput().getResults() == null
                    || result.getOutput().getResults().isEmpty()) {
                throw new IllegalStateException("Empty image result");
            }

            // ✅ 关键修复：DashScope 新版 SDK 里 results 是 Map
            Object first = result.getOutput().getResults().get(0);
            if (first instanceof Map<?, ?> map) {
                Object url = map.get("url");
                if (url != null) {
                    return url.toString();
                }
            }

            throw new IllegalStateException("Image url not found in result: " + first);

        } catch (Exception e) {
            throw new RuntimeException("Image generation failed: " + e.getMessage(), e);
        }
    }

}
