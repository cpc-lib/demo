package cc.ivera.ragdemo.domain.pojo;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_order")
public class WorkOrder {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("ticket_no")
    private String ticketNo;

    @TableField("title")
    private String title;

    @TableField("status")
    private String status;

    @TableField("priority")
    private String priority;

    @TableField("assignee_id")
    private String assigneeId;

    @TableField("assignee")
    private String assignee;

    @TableField("creator")
    private String creator;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}