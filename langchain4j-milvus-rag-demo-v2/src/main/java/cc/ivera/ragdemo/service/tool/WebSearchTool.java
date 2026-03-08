package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class WebSearchTool {

    private final TavilyWebSearchClient tavilyWebSearchClient;

    @Tool("查询最新互联网信息。仅当知识库未命中、信息不足，或用户问题需要最新外部事实、新闻、官网资料、公开文档时使用。返回前10条结果和来源。")
    public String webSearch(String query) {
        List<SourceItem> items = tavilyWebSearchClient.searchTop10(query);
        if (items.isEmpty()) {
            AgentTraceContext.current().addToolTrace("webSearch", "未配置 Tavily API Key 或未检索到结果");
            return "WEB_SEARCH_NOT_AVAILABLE";
        }

        AgentTraceContext.current().setWebSearchUsed(true);
        AgentTraceContext.current().addSources(items);
        AgentTraceContext.current().addToolTrace("webSearch", "互联网返回结果数=" + items.size());

        return items.stream()
                .limit(10)
                .map(item -> "[互联网来源] 标题=" + safe(item.title())
                        + "\nURL=" + safe(item.url())
                        + "\n摘要=" + safe(item.content()))
                .collect(Collectors.joining("\n\n---\n\n"));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
