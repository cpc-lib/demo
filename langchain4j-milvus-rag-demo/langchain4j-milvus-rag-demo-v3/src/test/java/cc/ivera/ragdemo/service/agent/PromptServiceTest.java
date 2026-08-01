package cc.ivera.ragdemo.service.agent;

import cc.ivera.ragdemo.domain.rag.RagAgentPrompt;
import cc.ivera.ragdemo.mapper.RagAgentPromptMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromptServiceTest {

    @Test
    void getActivePromptFallsBackToGlobalDefaultWhenTenantPromptMissing() {
        RagAgentPromptMapper mapper = mock(RagAgentPromptMapper.class);
        PromptService service = new PromptService(mapper, mockProvider(null));
        RagAgentPrompt globalPrompt = prompt(11L, 0L, "default", "global system prompt", 3, 1);
        AtomicInteger selectOneCalls = new AtomicInteger();
        when(mapper.selectOne(any(Wrapper.class))).thenAnswer(invocation -> {
            int call = selectOneCalls.incrementAndGet();
            return call == 1 ? null : globalPrompt;
        });

        RagAgentPrompt activePrompt = service.getActivePrompt(7L);

        assertThat(activePrompt).isSameAs(globalPrompt);
        assertThat(selectOneCalls).hasValue(2);
    }

    @Test
    void createPromptCanSaveDisabledVersionWithoutDisablingCurrentActivePrompt() {
        RagAgentPromptMapper mapper = mock(RagAgentPromptMapper.class);
        PromptService service = new PromptService(mapper, mockProvider(null));
        RagAgentPrompt active = prompt(1L, 7L, "default", "active prompt", 1, 1);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(active));
        when(mapper.insert(any(RagAgentPrompt.class))).thenReturn(1);

        RagAgentPrompt created = service.createPrompt(7L, "default", "draft prompt", false, "admin");

        ArgumentCaptor<RagAgentPrompt> insertCaptor = ArgumentCaptor.forClass(RagAgentPrompt.class);
        verify(mapper).insert(insertCaptor.capture());
        verify(mapper, never()).updateById(any(RagAgentPrompt.class));
        assertThat(created.getVersion()).isEqualTo(2);
        assertThat(created.getStatus()).isZero();
        assertThat(insertCaptor.getValue().getStatus()).isZero();
        assertThat(insertCaptor.getValue().getPromptContent()).isEqualTo("draft prompt");
    }

    @Test
    void enablePromptDisablesOtherActivePromptsForSameTenantAndPromptName() {
        RagAgentPromptMapper mapper = mock(RagAgentPromptMapper.class);
        PromptService service = new PromptService(mapper, mockProvider(null));
        RagAgentPrompt target = prompt(2L, 7L, "default", "candidate", 2, 0);
        RagAgentPrompt current = prompt(1L, 7L, "default", "current", 1, 1);
        when(mapper.selectById(2L)).thenReturn(target);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(current));
        when(mapper.updateById(any(RagAgentPrompt.class))).thenReturn(1);

        RagAgentPrompt enabled = service.enablePrompt(7L, 2L, "admin");

        ArgumentCaptor<RagAgentPrompt> updateCaptor = ArgumentCaptor.forClass(RagAgentPrompt.class);
        verify(mapper, org.mockito.Mockito.times(2)).updateById(updateCaptor.capture());
        assertThat(enabled.getStatus()).isEqualTo(1);
        assertThat(updateCaptor.getAllValues())
                .extracting(RagAgentPrompt::getId)
                .containsExactly(1L, 2L);
        assertThat(updateCaptor.getAllValues().get(0).getStatus()).isZero();
        assertThat(updateCaptor.getAllValues().get(1).getStatus()).isEqualTo(1);
    }

    @Test
    void updateActivePromptCreatesNewEnabledVersionAndDisablesOldActivePrompt() {
        RagAgentPromptMapper mapper = mock(RagAgentPromptMapper.class);
        PromptService service = new PromptService(mapper, mockProvider(null));
        RagAgentPrompt active = prompt(1L, 7L, "default", "old prompt", 1, 1);
        when(mapper.selectById(1L)).thenReturn(active);
        when(mapper.selectList(any(Wrapper.class))).thenReturn(List.of(active));
        when(mapper.updateById(any(RagAgentPrompt.class))).thenReturn(1);
        when(mapper.insert(any(RagAgentPrompt.class))).thenReturn(1);

        RagAgentPrompt updated = service.updatePromptById(7L, 1L, "default", "new prompt", true, "admin");

        ArgumentCaptor<RagAgentPrompt> insertCaptor = ArgumentCaptor.forClass(RagAgentPrompt.class);
        verify(mapper).insert(insertCaptor.capture());
        verify(mapper).updateById(active);
        assertThat(active.getStatus()).isZero();
        assertThat(updated.getVersion()).isEqualTo(2);
        assertThat(updated.getStatus()).isEqualTo(1);
        assertThat(insertCaptor.getValue().getPromptContent()).isEqualTo("new prompt");
    }

    @Test
    void disablePromptDisablesTenantOwnedPrompt() {
        RagAgentPromptMapper mapper = mock(RagAgentPromptMapper.class);
        PromptService service = new PromptService(mapper, mockProvider(null));
        RagAgentPrompt active = prompt(1L, 7L, "default", "current", 1, 1);
        when(mapper.selectById(1L)).thenReturn(active);
        when(mapper.updateById(any(RagAgentPrompt.class))).thenReturn(1);

        RagAgentPrompt disabled = service.disablePrompt(7L, 1L, "admin");

        verify(mapper).updateById(active);
        verify(mapper, never()).insert(any(RagAgentPrompt.class));
        assertThat(disabled.getStatus()).isZero();
        assertThat(disabled.getUpdatedBy()).isEqualTo("admin");
    }

    private static RagAgentPrompt prompt(
            Long id,
            Long tenantId,
            String promptName,
            String content,
            Integer version,
            Integer status
    ) {
        RagAgentPrompt prompt = new RagAgentPrompt();
        prompt.setId(id);
        prompt.setTenantId(tenantId);
        prompt.setPromptName(promptName);
        prompt.setPromptContent(content);
        prompt.setVersion(version);
        prompt.setStatus(status);
        return prompt;
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> mockProvider(T value) {
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }
}
