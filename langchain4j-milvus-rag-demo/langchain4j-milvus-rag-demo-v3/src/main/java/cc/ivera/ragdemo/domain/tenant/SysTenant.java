package cc.ivera.ragdemo.domain.tenant;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_tenant")
public class SysTenant {
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("tenant_code")
    private String tenantCode;
    @TableField("tenant_name")
    private String tenantName;
    @TableField("external_id")
    private String externalId;
    @TableField("status")
    private Integer status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;
    @TableField("is_deleted")
    private Integer isDeleted;
}
