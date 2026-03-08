package cc.ivera.ragdemo.service.trace;

import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.ToolTrace;

import java.util.ArrayList;
import java.util.List;

public final class AgentTraceContext {

    private static final ThreadLocal<TraceState> HOLDER = new ThreadLocal<>();

    private AgentTraceContext() {
    }

    public static void init(String question) {
        TraceState state = new TraceState();
        state.setQuestion(question);
        HOLDER.set(state);
    }

    public static TraceState current() {
        TraceState state = HOLDER.get();
        if (state == null) {
            state = new TraceState();
            HOLDER.set(state);
        }
        return state;
    }

    public static TraceState get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static class TraceState {
        private String question;
        private boolean knowledgeHit;
        private boolean webSearchUsed;
        private boolean weatherUsed;
        private final List<SourceItem> sources = new ArrayList<>();
        private final List<ToolTrace> toolTraces = new ArrayList<>();

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public boolean isKnowledgeHit() {
            return knowledgeHit;
        }

        public void setKnowledgeHit(boolean knowledgeHit) {
            this.knowledgeHit = knowledgeHit;
        }

        public boolean isWebSearchUsed() {
            return webSearchUsed;
        }

        public void setWebSearchUsed(boolean webSearchUsed) {
            this.webSearchUsed = webSearchUsed;
        }

        public boolean isWeatherUsed() {
            return weatherUsed;
        }

        public void setWeatherUsed(boolean weatherUsed) {
            this.weatherUsed = weatherUsed;
        }

        public List<SourceItem> getSources() {
            return sources;
        }

        public List<ToolTrace> getToolTraces() {
            return toolTraces;
        }

        public void addSources(List<SourceItem> sourceItems) {
            if (sourceItems == null || sourceItems.isEmpty()) {
                return;
            }
            this.sources.addAll(sourceItems);
        }

        public void addToolTrace(String toolName, String summary) {
            this.toolTraces.add(ToolTrace.builder()
                    .toolName(toolName)
                    .summary(summary)
                    .build());
        }
    }
}
