package cc.ivera.ragdemo.domain.vo;

import cc.ivera.ragdemo.domain.dto.TicketDetailItem;
import cc.ivera.ragdemo.domain.dto.TicketStatusItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketAnalysisResult {
    private Long totalCount;
    private List<TicketStatusItem> statusDistribution;
    /**
     * 工单明细列表
     */
    private List<TicketDetailItem> tickets;
}