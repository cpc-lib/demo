package cc.ivera.service.audit;

import cc.ivera.audit.annotation.LogField;
import cc.ivera.audit.model.TrackedFieldDefinition;
import cc.ivera.dto.ChangeLogResponse;
import cc.ivera.entity.ChangeLogDetailRecord;
import cc.ivera.entity.ChangeLogRecord;
import cc.ivera.mapper.ChangeLogDetailMapper;
import cc.ivera.mapper.ChangeLogMapper;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChangeLogService {

    private static final String OP_CREATE = "CREATE";
    private static final String OP_UPDATE = "UPDATE";
    private static final String OP_DELETE = "DELETE";

    private final ChangeLogMapper changeLogMapper;
    private final ChangeLogDetailMapper changeLogDetailMapper;

    public List<TrackedFieldDefinition> resolveFromAnnotation(Class<?> entityClass) {
        List<TrackedFieldDefinition> fields = new ArrayList<>();
        for (Field field : entityClass.getDeclaredFields()) {
            LogField logField = field.getAnnotation(LogField.class);
            if (logField != null && logField.enabled()) {
                fields.add(new TrackedFieldDefinition(field.getName(), logField.label()));
            }
        }
        return fields;
    }

    @Transactional(rollbackFor = Exception.class)
    public void record(String bizType,
                       String bizId,
                       String operationType,
                       String operator,
                       String methodName,
                       String requestUri,
                       String logMode,
                       Object before,
                       Object after,
                       List<TrackedFieldDefinition> trackedFields) {
        List<ChangeLogDetailRecord> details = buildDetails(before, after, trackedFields);
        int changedFieldCount = (int) details.stream().filter(item -> item.getChanged() == 1).count();

        ChangeLogRecord logRecord = new ChangeLogRecord();
        logRecord.setBizType(bizType);
        logRecord.setBizId(bizId);
        logRecord.setOperationType(operationType);
        logRecord.setLogMode(logMode);
        logRecord.setOperator(operator);
        logRecord.setMethodName(methodName);
        logRecord.setRequestUri(requestUri);
        logRecord.setChangedFieldCount(changedFieldCount);
        logRecord.setRemark(resolveRemark(operationType, changedFieldCount));
        logRecord.setCreatedAt(LocalDateTime.now());
        changeLogMapper.insert(logRecord);

        for (ChangeLogDetailRecord detail : details) {
            detail.setLogId(logRecord.getId());
            changeLogDetailMapper.insert(detail);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void recordUpdate(String bizType,
                             String bizId,
                             String operator,
                             String methodName,
                             String requestUri,
                             String logMode,
                             Object before,
                             Object after,
                             List<TrackedFieldDefinition> trackedFields) {
        record(bizType, bizId, OP_UPDATE, operator, methodName, requestUri, logMode, before, after, trackedFields);
    }

    public List<ChangeLogResponse> listByBiz(String bizType, String bizId) {
        List<ChangeLogRecord> records = changeLogMapper.selectList(
                new LambdaQueryWrapper<ChangeLogRecord>()
                        .eq(ChangeLogRecord::getBizType, bizType)
                        .eq(ChangeLogRecord::getBizId, bizId)
                        .orderByDesc(ChangeLogRecord::getId)
        );
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> logIds = records.stream().map(ChangeLogRecord::getId).toList();
        List<ChangeLogDetailRecord> detailRecords = changeLogDetailMapper.selectList(
                new LambdaQueryWrapper<ChangeLogDetailRecord>()
                        .in(ChangeLogDetailRecord::getLogId, logIds)
                        .orderByAsc(ChangeLogDetailRecord::getId)
        );

        Map<Long, List<ChangeLogDetailRecord>> detailMap = detailRecords.stream()
                .collect(Collectors.groupingBy(ChangeLogDetailRecord::getLogId));

        List<ChangeLogResponse> result = new ArrayList<>();
        for (ChangeLogRecord record : records) {
            ChangeLogResponse response = new ChangeLogResponse();
            response.setLogId(record.getId());
            response.setBizType(record.getBizType());
            response.setBizId(record.getBizId());
            response.setOperationType(record.getOperationType());
            response.setLogMode(record.getLogMode());
            response.setOperator(record.getOperator());
            response.setChangedFieldCount(record.getChangedFieldCount());
            response.setRemark(record.getRemark());
            response.setCreatedAt(record.getCreatedAt());

            List<ChangeLogResponse.ChangeLogDetailItem> items = detailMap.getOrDefault(record.getId(), Collections.emptyList())
                    .stream()
                    .map(this::toResponseItem)
                    .toList();
            response.setDetails(items);
            result.add(response);
        }
        return result;
    }

    private String resolveRemark(String operationType, int changedFieldCount) {
        return switch (operationType) {
            case OP_CREATE -> changedFieldCount > 0 ? "记录新增字段快照" : "本次新增未采集到字段快照";
            case OP_DELETE -> changedFieldCount > 0 ? "记录删除前字段快照" : "本次删除未采集到字段快照";
            case OP_UPDATE -> changedFieldCount > 0 ? "记录更新字段变更" : "本次更新未产生字段差异";
            default -> "记录业务操作日志";
        };
    }

    private ChangeLogResponse.ChangeLogDetailItem toResponseItem(ChangeLogDetailRecord detail) {
        ChangeLogResponse.ChangeLogDetailItem item = new ChangeLogResponse.ChangeLogDetailItem();
        item.setFieldName(detail.getFieldName());
        item.setFieldLabel(detail.getFieldLabel());
        item.setOldValue(detail.getOldValue());
        item.setNewValue(detail.getNewValue());
        item.setChanged(detail.getChanged());
        return item;
    }

    private List<ChangeLogDetailRecord> buildDetails(Object before, Object after, List<TrackedFieldDefinition> trackedFields) {
        List<ChangeLogDetailRecord> details = new ArrayList<>();
        for (TrackedFieldDefinition trackedField : trackedFields) {
            Object oldValue = getFieldValue(before, trackedField.getFieldName());
            Object newValue = getFieldValue(after, trackedField.getFieldName());
            String oldValueText = stringify(oldValue);
            String newValueText = stringify(newValue);

            ChangeLogDetailRecord detail = new ChangeLogDetailRecord();
            detail.setFieldName(trackedField.getFieldName());
            detail.setFieldLabel(trackedField.getLabel());
            detail.setOldValue(oldValueText);
            detail.setNewValue(newValueText);
            detail.setValueType(resolveValueType(oldValue, newValue));
            detail.setChanged(Objects.equals(oldValueText, newValueText) ? 0 : 1);
            detail.setCreatedAt(LocalDateTime.now());
            details.add(detail);
        }
        return details;
    }

    private Object getFieldValue(Object source, String fieldName) {
        if (source == null) {
            return null;
        }
        Class<?> clazz = source.getClass();
        while (clazz != null && clazz != Object.class) {
            try {
                Field field = clazz.getDeclaredField(fieldName);
                field.setAccessible(true);
                return field.get(source);
            } catch (NoSuchFieldException ignored) {
                clazz = clazz.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + fieldName, e);
            }
        }
        return null;
    }

    private String stringify(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof CharSequence || value instanceof Number || value instanceof Boolean) {
            return String.valueOf(value);
        }
        return JSON.toJSONString(value);
    }

    private String resolveValueType(Object oldValue, Object newValue) {
        Object value = newValue != null ? newValue : oldValue;
        return value == null ? "null" : value.getClass().getSimpleName();
    }
}
