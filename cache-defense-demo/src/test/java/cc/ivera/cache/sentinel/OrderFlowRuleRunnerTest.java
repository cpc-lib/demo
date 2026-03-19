package cc.ivera.cache.sentinel;

import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.context.ContextUtil;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

class OrderFlowRuleRunnerTest {

    @AfterEach
    void tearDown() {
        ParamFlowRuleManager.loadRules(java.util.List.of());
        FlowRuleManager.loadRules(java.util.List.of());
        ContextUtil.exit();
    }

    @Test
    void loadsOrderQueryRule() throws Exception {
        OrderFlowRuleRunner runner = new OrderFlowRuleRunner(5D);

        runner.run();

        assertThat(ParamFlowRuleManager.getRulesOfResource(OrderBlockHandler.GET_ORDER_RESOURCE))
                .extracting(ParamFlowRule::getParamIdx, ParamFlowRule::getCount)
                .contains(tuple(0, 5D));
        assertThat(FlowRuleManager.getRules())
                .filteredOn(rule -> OrderBlockHandler.GET_ORDER_RESOURCE.equals(rule.getResource()))
                .isEmpty();
    }

    @Test
    void limitsPerClientIpAndOrderIdCompositeKey() throws Exception {
        OrderFlowRuleRunner runner = new OrderFlowRuleRunner(1D);

        runner.run();
        ContextUtil.enter("order-query-test");
        try {
            assertThatCode(() -> {
                try (var ignored = SphU.entry(OrderBlockHandler.GET_ORDER_RESOURCE, EntryType.IN, 1, "127.0.0.1:1")) {
                    // first hit should pass
                }
            }).doesNotThrowAnyException();

            assertThatThrownBy(() -> SphU.entry(OrderBlockHandler.GET_ORDER_RESOURCE, EntryType.IN, 1, "127.0.0.1:1"))
                    .isInstanceOf(BlockException.class);

            assertThatCode(() -> {
                try (var ignored = SphU.entry(OrderBlockHandler.GET_ORDER_RESOURCE, EntryType.IN, 1, "127.0.0.1:2")) {
                    // different order id should use a different bucket
                }
            }).doesNotThrowAnyException();

            assertThatCode(() -> {
                try (var ignored = SphU.entry(OrderBlockHandler.GET_ORDER_RESOURCE, EntryType.IN, 1, "127.0.0.2:1")) {
                    // different client ip should use a different bucket
                }
            }).doesNotThrowAnyException();
        } finally {
            ContextUtil.exit();
        }
    }
}
