package cc.ivera.ragdemo.service.tool;


import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TextToImageTool {

    private final OpenAiImageClient openAiImageClient;

    @Tool("""
            根据用户描述生成图片。
            参数：
            - prompt: 图片描述提示词（必填，尽量具体）
            - size: 图片尺寸（可选，例如 1024x1024 / 1024x1792 / 1792x1024）
            返回：图片URL与生成结果说明。
            """)
    public String textToImage(@P("图片描述提示词，必填，需包含主体、风格、场景等关键信息") String prompt,
                              @P("图片尺寸，可选，如 1024x1024 / 1024x1792 / 1792x1024") String size) {
        if (!StringUtils.hasText(prompt)) {
            AgentTraceContext.current().addToolTrace("textToImage", "参数缺失: prompt");
            return "生成图片失败：缺少 prompt。请提供更具体的图片描述后再试。";
        }
        OpenAiImageClient.ImageResult result = openAiImageClient.generate(prompt, size);
        AgentTraceContext.current().addSources(result.sources());
        AgentTraceContext.current().addToolTrace("textToImage", "prompt=" + prompt + ", size=" + (size == null ? "" : size));
        if (StringUtils.hasText(result.imageUrl())) {
            return result.imageUrl();
        }
        return result.summary();
    }
}
