package com.demo.order.job;

import com.demo.order.mapper.JobCronConfigMapper;
import com.demo.order.service.OrderTimeoutCloseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.var;
import org.apache.shardingsphere.elasticjob.api.ShardingContext;
import org.apache.shardingsphere.elasticjob.simple.job.SimpleJob;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticCloseTimeoutOrderJob implements SimpleJob {
    private final OrderTimeoutCloseService closeService;
    private final JobCronConfigMapper cronConfigMapper;

    @Override
    public void execute(ShardingContext shardingContext) {
        var config = cronConfigMapper.selectByJobType("ELASTIC");
        if (config == null || config.getEnabled() == 0) {
            log.info("ElasticJob disabled by mysql config");
            return;
        }
        int closed = closeService.closeTimeoutUnpaidOrders("ELASTIC");
        log.info("ElasticJob closed timeout unpaid orders: {}, shard={}/{}", closed, shardingContext.getShardingItem(), shardingContext.getShardingTotalCount());
    }
}
