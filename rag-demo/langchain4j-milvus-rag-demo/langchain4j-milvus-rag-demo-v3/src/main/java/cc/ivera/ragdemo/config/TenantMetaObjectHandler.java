package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

/**
 * Auto-fills {@code tenant_id} on insert for entities that declare a
 * {@code tenantId} field annotated with
 * {@code @TableField(value = "tenant_id", fill = FieldFill.INSERT)}.
 *
 * <p>If the entity already has a non-null {@code tenantId} (set explicitly by
 * service code, e.g. derived from a parent entity), the handler does NOT
 * overwrite it. This keeps the existing parent-derived tenant assignment
 * pattern working while ensuring child/standalone entities always get a
 * tenant_id even when the service forgets to set it manually.
 *
 * <p>On update, {@code tenant_id} is never modified — once assigned, the
 * tenant ownership of a row is immutable.
 */
@Component
public class TenantMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        if (metaObject.hasSetter("tenantId")) {
            Object existing = getFieldValByName("tenantId", metaObject);
            if (existing == null) {
                Long tenantId = TenantContextHolder.currentTenantId().orElse(0L);
                setFieldValByName("tenantId", tenantId, metaObject);
            }
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        // tenant_id is immutable after insert — never auto-fill on update
    }
}
