package cc.ivera.service.reconciliation.channel;

import cc.ivera.exception.BizException;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class ChannelReconciliationStrategyFactory {

    private final Map<String, ChannelReconciliationStrategy> strategyMap = new HashMap<>();

    public ChannelReconciliationStrategyFactory(List<ChannelReconciliationStrategy> strategies) {
        for (ChannelReconciliationStrategy strategy : strategies) {
            strategyMap.put(strategy.getChannelCode(), strategy);
        }
    }

    public ChannelReconciliationStrategy getStrategy(String channelCode) {
        ChannelReconciliationStrategy strategy = strategyMap.get(channelCode);
        if (strategy == null) {
            throw new BizException("不支持的对账渠道：" + channelCode);
        }
        return strategy;
    }
}
