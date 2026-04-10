package cc.ivera.audit.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TrackedFieldDefinition {
    private String fieldName;
    private String label;
}
