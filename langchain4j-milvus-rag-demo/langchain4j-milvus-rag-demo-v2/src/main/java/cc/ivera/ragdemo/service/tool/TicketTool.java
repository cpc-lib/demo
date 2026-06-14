package cc.ivera.ragdemo.service.tool;

import cc.ivera.ragdemo.domain.dto.TicketDetailItem;
import cc.ivera.ragdemo.domain.dto.TicketStatusItem;
import cc.ivera.ragdemo.domain.vo.TicketAnalysisResult;
import cc.ivera.ragdemo.mapper.WorkOrderMapper;
import cc.ivera.ragdemo.model.SourceItem;
import cc.ivera.ragdemo.model.SourceType;
import cc.ivera.ragdemo.service.trace.AgentTraceContext;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TicketTool {

    private final WorkOrderMapper workOrderMapper;

    @Tool("""
            查询系统内部工单统计信息及工单明细。
            当提供 assigneeId 时，按处理人用户ID精确查询。
            适用于：
            1. 查询工单总数量
            2. 查询某个状态下的工单数量
            3. 查询某个处理人用户ID对应的工单数量
            4. 查询工单处理状态分布情况
            5. 查询指定时间范围内的工单数量和状态情况
            6. 查询符合条件的工单详细列表

            参数说明：
            - assigneeId: 处理人用户ID，建议优先传该值进行精确查询
            - status: 工单状态，可选 PENDING/PROCESSING/RESOLVED/CLOSED，也可传中文待处理/处理中/已解决/已关闭
            - priority: 优先级，可选 LOW/MEDIUM/HIGH/URGENT，也可传中文低/中/高/紧急
            - startTime: 开始时间，格式 yyyy-MM-dd'T'HH:mm:ss
            - endTime: 结束时间，格式 yyyy-MM-dd'T'HH:mm:ss
            - limit: 返回工单明细条数限制，建议 10~50
            """)
    public TicketAnalysisResult ticketAnalysis(String assigneeId,
                                               String status,
                                               String priority,
                                               String startTime,
                                               String endTime,
                                               Integer limit) {

        String normalizedAssigneeId = blankToNull(assigneeId);
        String normalizedStatus = normalizeStatus(status);
        String normalizedPriority = normalizePriority(priority);
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        Integer safeLimit = normalizeLimit(limit);

        long totalCount = workOrderMapper.countTickets(
                normalizedAssigneeId,
                normalizedStatus,
                normalizedPriority,
                start,
                end
        );

        List<TicketStatusItem> distribution = workOrderMapper.statusDistribution(
                normalizedAssigneeId,
                normalizedStatus,
                normalizedPriority,
                start,
                end
        );

        List<TicketDetailItem> tickets = workOrderMapper.queryTicketDetails(
                normalizedAssigneeId,
                normalizedStatus,
                normalizedPriority,
                start,
                end,
                safeLimit
        );

        AgentTraceContext.current().addToolTrace(
                "ticketAnalysis",
                "工单统计+明细查询, assigneeId=" + normalizedAssigneeId
                        + ", status=" + normalizedStatus
                        + ", priority=" + normalizedPriority
                        + ", startTime=" + start
                        + ", endTime=" + end
                        + ", limit=" + safeLimit
        );

        AgentTraceContext.current().addSources(List.of(
                SourceItem.builder()
                        .type(SourceType.KNOWLEDGE_BASE)
                        .title("内部工单系统")
                        .fileName("work_order")
                        .chunkId(null)
                        .content("已查询内部工单系统统计数据及工单明细")
                        .build()
        ));

        return new TicketAnalysisResult(totalCount, distribution, tickets);
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String s = status.trim().toUpperCase();
        return switch (s) {
            case "待处理", "PENDING", "NEW" -> "PENDING";
            case "处理中", "PROCESSING" -> "PROCESSING";
            case "已解决", "RESOLVED", "DONE" -> "RESOLVED";
            case "已关闭", "CLOSED" -> "CLOSED";
            default -> s;
        };
    }

    private String normalizePriority(String priority) {
        if (priority == null || priority.isBlank()) {
            return null;
        }
        String p = priority.trim().toUpperCase();
        return switch (p) {
            case "低", "LOW" -> "LOW";
            case "中", "MEDIUM" -> "MEDIUM";
            case "高", "HIGH" -> "HIGH";
            case "紧急", "URGENT" -> "URGENT";
            default -> p;
        };
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }

    private LocalDateTime parseDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return LocalDateTime.parse(value.trim());
    }

    private Integer normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return 10;
        }
        return Math.min(limit, 100);
    }
}