package cc.ivera.ragdemo.service.tenant;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * Event fired when model configuration is changed in the database.
 * Used to notify the DynamicModelFactory to invalidate cached models.
 */
@Getter
public class ModelConfigChangedEvent extends ApplicationEvent {

    private final Long tenantId;
    private final String modelType;

    public ModelConfigChangedEvent(Object source, Long tenantId, String modelType) {
        super(source);
        this.tenantId = tenantId;
        this.modelType = modelType;
    }
}
