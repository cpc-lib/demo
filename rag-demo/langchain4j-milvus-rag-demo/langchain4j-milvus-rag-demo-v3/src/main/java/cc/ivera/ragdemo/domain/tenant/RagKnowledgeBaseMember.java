package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rag_knowledge_base_member")
public class RagKnowledgeBaseMember {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField(value = "tenant_id", fill = FieldFill.INSERT)
    private Long tenantId;
    @TableField("knowledge_base_id")
    private Long knowledgeBaseId;
    @TableField("user_id")
    private String userId;
    @TableField("member_role")
    private String memberRole;
    @TableField("permission_tags")
    private String permissionTags;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @TableField("is_deleted")
    private Integer isDeleted;
}
