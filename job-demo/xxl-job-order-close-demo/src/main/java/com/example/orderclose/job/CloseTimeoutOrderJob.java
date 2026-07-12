package com.example.orderclose.job;

import com.example.orderclose.service.OrderCloseResult;
import com.example.orderclose.service.OrderCloseService;
import com.xxl.job.core.context.XxlJobHelper;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class CloseTimeoutOrderJob {

    private static final Logger log = LoggerFactory.getLogger(CloseTimeoutOrderJob.class);
    private static final int DEFAULT_BATCH_SIZE = 200;
    private static final int DEFAULT_MAX_ROUNDS = 20;

    private final OrderCloseService orderCloseService;

    public CloseTimeoutOrderJob(OrderCloseService orderCloseService) {
        this.orderCloseService = orderCloseService;
    }

    /**
     * XXL-JOB handler name configured in the admin console:
     * closeTimeoutOrderJobHandler
     *
     * Supported job parameter:
     * batchSize=200,maxRounds=20
     */
    @XxlJob("closeTimeoutOrderJobHandler")
    public void execute() {
        String rawParam = XxlJobHelper.getJobParam();
        JobParam param = JobParam.parse(rawParam);

        XxlJobHelper.log("Start timeout order close job. batchSize={}, maxRounds={}, rawParam={}",
                param.batchSize(), param.maxRounds(), rawParam);

        try {
            OrderCloseResult result = orderCloseService.closeTimeoutOrders(
                    param.batchSize(), param.maxRounds());

            String message = "Timeout order close completed: scanned=%d, closed=%d, skipped=%d, rounds=%d, reachedLimit=%s"
                    .formatted(result.scanned(), result.closed(), result.skipped(),
                            result.rounds(), result.reachedLimit());

            log.info(message);
            XxlJobHelper.log(message);
            XxlJobHelper.handleSuccess(message);
        } catch (Exception ex) {
            log.error("Timeout order close job failed", ex);
            XxlJobHelper.log("Timeout order close failed: {}", ex.getMessage());
            XxlJobHelper.handleFail(ex.getMessage());
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("Timeout order close job failed", ex);
        }
    }

    record JobParam(int batchSize, int maxRounds) {

        static JobParam parse(String raw) {
            if (raw == null || raw.isBlank()) {
                return new JobParam(DEFAULT_BATCH_SIZE, DEFAULT_MAX_ROUNDS);
            }

            Map<String, String> values = new HashMap<>();
            for (String pair : raw.split(",")) {
                String[] parts = pair.trim().split("=", 2);
                if (parts.length == 2 && !parts[0].isBlank()) {
                    values.put(parts[0].trim(), parts[1].trim());
                }
            }

            int batchSize = parsePositiveInt(values.get("batchSize"), DEFAULT_BATCH_SIZE, "batchSize");
            int maxRounds = parsePositiveInt(values.get("maxRounds"), DEFAULT_MAX_ROUNDS, "maxRounds");
            return new JobParam(batchSize, maxRounds);
        }

        private static int parsePositiveInt(String raw, int defaultValue, String name) {
            if (raw == null || raw.isBlank()) {
                return defaultValue;
            }
            try {
                int value = Integer.parseInt(raw);
                if (value <= 0) {
                    throw new IllegalArgumentException(name + " must be greater than 0");
                }
                return value;
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException(name + " must be an integer", ex);
            }
        }
    }
}
