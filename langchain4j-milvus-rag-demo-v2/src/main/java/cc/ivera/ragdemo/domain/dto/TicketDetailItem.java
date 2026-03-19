package cc.ivera.ragdemo.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDetailItem {

    private Long id;
    private String ticketNo;
    private String title;
    private String status;
    private String priority;
    private String assigneeId;
    private String assignee;
    private String creator;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}