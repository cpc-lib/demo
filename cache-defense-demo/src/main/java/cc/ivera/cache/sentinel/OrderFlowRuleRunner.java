package cc.ivera.cache.sentinel;

import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class OrderFlowRuleRunner implements CommandLineRunner {

    private final double orderQueryKeyQps;

    public OrderFlowRuleRunner(@Value("${app.sentinel.order-query-key-qps:${app.sentinel.order-query-qps:5}}") double orderQueryKeyQps) {
        this.orderQueryKeyQps = orderQueryKeyQps;
    }

    @Override
    public void run(String... args) {
        removeLegacyFlowRules();

        List<ParamFlowRule> rules = new ArrayList<>(ParamFlowRuleManager.getRules());
        rules.removeIf(existing -> OrderBlockHandler.GET_ORDER_RESOURCE.equals(existing.getResource()));

        if (orderQueryKeyQps <= 0) {
            ParamFlowRuleManager.loadRules(rules);
            log.info("Sentinel param flow rule for {} is disabled", OrderBlockHandler.GET_ORDER_RESOURCE);
            return;
        }

        ParamFlowRule rule = new ParamFlowRule(OrderBlockHandler.GET_ORDER_RESOURCE);
        rule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        rule.setParamIdx(0);
        rule.setCount(orderQueryKeyQps);

        rules.add(rule);
        ParamFlowRuleManager.loadRules(rules);

        log.info("Sentinel param flow rule loaded for {}, key qps={}", OrderBlockHandler.GET_ORDER_RESOURCE, orderQueryKeyQps);
    }

    private void removeLegacyFlowRules() {
        List<FlowRule> flowRules = new ArrayList<>(FlowRuleManager.getRules());
        flowRules.removeIf(existing -> OrderBlockHandler.GET_ORDER_RESOURCE.equals(existing.getResource()));
        FlowRuleManager.loadRules(flowRules);
    }
}
