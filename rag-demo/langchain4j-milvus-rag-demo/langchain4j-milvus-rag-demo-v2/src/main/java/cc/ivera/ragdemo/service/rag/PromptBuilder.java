package cc.ivera.ragdemo.service.rag;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PromptBuilder {

    public String build(String question, List<String> contextChunks) {
        String context = String.join("\n\n---\n\n", contextChunks);

        return ("""
                你是企业级知识库助手。请严格基于【上下文】回答【问题】。
                - 如果上下文没有相关信息，请明确回答“我不知道”，不要编造。
                - 回答要尽量简洁、可操作、条理清晰（可用要点列表）。
                
                【上下文】
                %s
                
                【问题】
                %s
                """).formatted(context, question);
    }
}
