package com.example.points.job;

import com.example.points.domain.PointBizNoGenerator;
import com.example.points.domain.PointRewardCommand;
import com.example.points.messaging.PointRewardProducer;
import com.example.points.repository.BatchRepository;
import com.example.points.repository.UserRepository;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
public class DailyPointRewardJob {
    private static final int PAGE_SIZE = 500;
    private static final long POINTS = 10L;
    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
    private final UserRepository users;
    private final BatchRepository batches;
    private final PointRewardProducer producer;

    public DailyPointRewardJob(UserRepository users, BatchRepository batches, PointRewardProducer producer) {
        this.users = users;
        this.batches = batches;
        this.producer = producer;
    }

    @XxlJob("dailyPointRewardJob")
    public void execute() {
        int shard = Math.max(0, XxlJobHelper.getShardIndex()), total = Math.max(1, XxlJobHelper.getShardTotal());
        LocalDate date = LocalDate.now(ZONE);
        long batchId = batches.getOrCreate(date, total);
        batches.markShardRunning(batchId, shard);
        long lastId = 0, sent = 0;
        try {
            while (true) {
                List<Long> ids = users.findShardUsers(lastId, shard, total, PAGE_SIZE);
                if (ids.isEmpty()) break;
                List<PointRewardCommand> commands = ids.stream().map(id -> new PointRewardCommand(batchId, id, PointBizNoGenerator.dailyReward(id, date), "DAILY_REWARD", POINTS, date)).toList();
                producer.sendBatch(commands);
                batches.increment(batchId, ids.size(), commands.size());
                sent += commands.size();
                lastId = ids.get(ids.size() - 1);
                XxlJobHelper.log("batch={}, shard={}/{}, lastId={}, sent={}", batchId, shard, total, lastId, sent);
                if (ids.size() < PAGE_SIZE) break;
            }
            batches.markShardSuccess(batchId, shard);
            batches.tryComplete(batchId);
        } catch (Exception e) {
            batches.markShardFailed(batchId, shard, e.getMessage());
            throw e;
        }
    }
}
