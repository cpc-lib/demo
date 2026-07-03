package cc.ivera.gray.admin.service;

import cc.ivera.gray.admin.entity.AuditLog;
import cc.ivera.gray.admin.mapper.AuditLogMapper;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogMapper auditLogMapper;

    public AuditService(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    public void record(String operator, String action, String resourceType, String resourceId, String beforeData, String afterData) {
        AuditLog log = new AuditLog();
        log.setOperator(operator == null || operator.isBlank() ? "system" : operator);
        log.setAction(action);
        log.setResourceType(resourceType);
        log.setResourceId(resourceId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        auditLogMapper.insert(log);
    }
}

