package com.example.orderjob.job;

import com.example.orderjob.domain.OrderCloseSummary;
import com.example.orderjob.service.ExpiredOrderCloseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import tech.powerjob.worker.core.processor.ProcessResult;
import tech.powerjob.worker.core.processor.TaskContext;
import tech.powerjob.worker.core.processor.sdk.BasicProcessor;
import tech.powerjob.worker.log.OmsLogger;

@Component("expiredUnpaidOrderCloseProcessor")
public class ExpiredUnpaidOrderCloseProcessor implements BasicProcessor {

    private final ObjectMapper objectMapper;
    private final ExpiredOrderCloseService closeService;

    public ExpiredUnpaidOrderCloseProcessor(ObjectMapper objectMapper,
                                            ExpiredOrderCloseService closeService) {
        this.objectMapper = objectMapper;
        this.closeService = closeService;
    }

    @Override
    public ProcessResult process(TaskContext context) {
        OmsLogger logger = context.getOmsLogger();
        try {
            String rawParam = firstNonBlank(context.getInstanceParams(), context.getJobParams());
            CloseJobParam param = parseParam(rawParam);
            String instanceId = context.getInstanceMeta() == null
                    ? "unknown"
                    : abbreviate(String.valueOf(context.getInstanceMeta()), 120);

            logger.info("Start closing expired unpaid orders. batchSize={}, maxPages={}, instanceId={}",
                    param.getBatchSize(), param.getMaxPages(), instanceId);

            OrderCloseSummary summary = closeService.execute(param, "POWERJOB", instanceId);
            logger.info("Expired unpaid order close completed: {}", summary.toMessage());
            return new ProcessResult(true, summary.toMessage());
        } catch (Exception e) {
            logger.error("Expired unpaid order close failed", e);
            return new ProcessResult(false, abbreviate(e.getMessage(), 1500));
        }
    }

    private CloseJobParam parseParam(String rawParam) throws Exception {
        if (rawParam == null || rawParam.isBlank()) {
            return new CloseJobParam();
        }
        CloseJobParam param = objectMapper.readValue(rawParam, CloseJobParam.class);
        param.validate();
        return param;
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }

    private static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "unknown error";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
