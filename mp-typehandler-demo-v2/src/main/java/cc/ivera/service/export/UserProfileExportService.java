package cc.ivera.service.export;

import cc.ivera.common.BusinessException;
import cc.ivera.config.ExportFileProperties;
import cc.ivera.dto.ExportTaskResponse;
import cc.ivera.dto.UserProfileExportRequest;
import cc.ivera.entity.ExportTaskRecord;
import cc.ivera.entity.UserProfile;
import cc.ivera.enums.ExportTaskStatus;
import cc.ivera.excel.UserProfileExportColumn;
import cc.ivera.mapper.ExportTaskMapper;
import cc.ivera.mapper.UserProfileMapper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProfileExportService {

    private static final String BUSINESS_TYPE = "USER_PROFILE";
    private static final int EXPORT_BATCH_SIZE = 500;
    private static final int MAX_EXPORT_COLUMNS = 20;
    private static final int MAX_ACTIVE_TASKS = 3;

    private final UserProfileMapper userProfileMapper;
    private final ExportTaskMapper exportTaskMapper;
    private final Executor exportTaskExecutor;
    private final ObjectMapper objectMapper;
    private final ExportFileProperties exportFileProperties;
    private final Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "mp-typehandler-demo-exports");

    public UserProfileExportService(UserProfileMapper userProfileMapper,
                                    ExportTaskMapper exportTaskMapper,
                                    @Qualifier("exportTaskExecutor") Executor exportTaskExecutor,
                                    ObjectMapper objectMapper,
                                    ExportFileProperties exportFileProperties) {
        this.userProfileMapper = userProfileMapper;
        this.exportTaskMapper = exportTaskMapper;
        this.exportTaskExecutor = exportTaskExecutor;
        this.objectMapper = objectMapper;
        this.exportFileProperties = exportFileProperties;
    }

    @PostConstruct
    public void init() {
        ensureDir();
        recoverInterruptedTasks();
    }

    public ExportTaskResponse submit(UserProfileExportRequest request) {
        validateRequest(request);
        ensureDir();
        ensureActiveTaskCapacity();

        ExportTaskRecord task = new ExportTaskRecord();
        String taskId = java.util.UUID.randomUUID().toString().replace("-", "");
        task.setTaskId(taskId);
        task.setBusinessType(BUSINESS_TYPE);
        task.setStatus(ExportTaskStatus.WAITING.name());
        task.setMessage("任务已提交，等待执行");
        task.setQueryName(request.getName());
        task.setPageNo(request.getPageNo());
        task.setPageSize(request.getPageSize());
        task.setFieldsJson(writeJson(request.getFields()));
        task.setTotalCount(0L);
        task.setExportedCount(0L);
        task.setCreatedAt(LocalDateTime.now());
        //设置过期时间
        task.setExpireAt(LocalDateTime.now().plusMinutes(exportFileProperties.getExpireMinutes()));
        exportTaskMapper.insert(task);

        try {
            exportTaskExecutor.execute(() -> doExport(task.getTaskId(), request));
        } catch (RejectedExecutionException ex) {
            markFailed(taskId, "导出队列已满，请稍后再试", null);
            throw new BusinessException("导出队列已满，请稍后再试");
        }
        return toResponse(getRequiredTask(taskId));
    }

    public ExportTaskResponse getTask(String taskId) {
        return toResponse(getRequiredTask(taskId));
    }

    public Resource getExportFile(String taskId) {
        ExportTaskRecord task = getRequiredTask(taskId);
        ExportTaskStatus status = ExportTaskStatus.valueOf(task.getStatus());
        if (status != ExportTaskStatus.SUCCESS) {
            throw new BusinessException("任务未完成，暂不可下载");
        }
        if (task.getExpireAt() != null && task.getExpireAt().isBefore(LocalDateTime.now())) {
            updateExpired(task);
            throw new BusinessException("文件已过期，请重新导出");
        }
        if (!StringUtils.hasText(task.getFilePath())) {
            throw new BusinessException("导出文件不存在，请重新导出");
        }
        Path filePath = Paths.get(task.getFilePath());
        if (!Files.exists(filePath)) {
            throw new BusinessException("导出文件不存在，请重新导出");
        }
        return new FileSystemResource(filePath);
    }



    public int cleanupExpiredTasks() {
        List<ExportTaskRecord> tasks = exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTaskRecord>()
                .eq(ExportTaskRecord::getBusinessType, BUSINESS_TYPE)
                .eq(ExportTaskRecord::getStatus, ExportTaskStatus.SUCCESS.name())
                .isNotNull(ExportTaskRecord::getExpireAt)
                .lt(ExportTaskRecord::getExpireAt, LocalDateTime.now()));
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        int cleaned = 0;
        for (ExportTaskRecord task : tasks) {
            updateExpired(task);
            cleaned++;
        }
        return cleaned;
    }

    private void doExport(String taskId, UserProfileExportRequest request) {
        List<UserProfileExportColumn> columns = parseColumns(request.getFields());
        ExportTaskRecord task = getRequiredTask(taskId);
        String fileName = "user-profile-export-" + taskId + ".xlsx";
        Path filePath = exportDir.resolve(fileName);

        task.setStatus(ExportTaskStatus.RUNNING.name());
        task.setMessage("任务执行中");
        task.setFileName(fileName);
        task.setFilePath(filePath.toString());
        task.setStartedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);

        try (ExcelWriter writer = EasyExcel.write(filePath.toFile())
                .head(buildHead(columns))
                .autoCloseStream(true)
                .build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("用户数据").build();

            if (isExportAll(request)) {
                exportAll(writer, writeSheet, taskId, request, columns);
            } else {
                exportOnePage(writer, writeSheet, taskId, request, columns);
            }

            ExportTaskRecord successTask = getRequiredTask(taskId);
            successTask.setStatus(ExportTaskStatus.SUCCESS.name());
            successTask.setMessage("导出成功");
            successTask.setFinishedAt(LocalDateTime.now());
            exportTaskMapper.updateById(successTask);
        } catch (Exception ex) {
            log.error("导出失败, taskId={}", taskId, ex);
            markFailed(taskId, "导出失败", ex.getMessage());
            try {
                Files.deleteIfExists(filePath);
            } catch (Exception deleteEx) {
                log.warn("删除失败文件失败, taskId={}", taskId, deleteEx);
            }
        }
    }

    private void exportOnePage(ExcelWriter writer, WriteSheet writeSheet, String taskId,
                               UserProfileExportRequest request, List<UserProfileExportColumn> columns) {
        Page<UserProfile> page = new Page<>(request.getPageNo(), request.getPageSize());
        LambdaQueryWrapper<UserProfile> wrapper = buildQueryWrapper(request.getName()).orderByDesc(UserProfile::getId);
        Page<UserProfile> result = userProfileMapper.selectPage(page, wrapper);
        writer.write(toRows(result.getRecords(), columns), writeSheet);
        updateProgress(taskId, result.getTotal(), (long) result.getRecords().size(), "分页导出完成");
    }

    private void exportAll(ExcelWriter writer, WriteSheet writeSheet, String taskId,
                           UserProfileExportRequest request, List<UserProfileExportColumn> columns) {
        long total = userProfileMapper.selectCount(buildQueryWrapper(request.getName()));
        updateProgress(taskId, total, 0L, total == 0 ? "无可导出数据" : "全量导出开始");

        Long lastId = 0L;
        long exported = 0L;
        while (true) {
            LambdaQueryWrapper<UserProfile> wrapper = buildQueryWrapper(request.getName())
                    .gt(UserProfile::getId, lastId)
                    .orderByAsc(UserProfile::getId)
                    .last("limit " + EXPORT_BATCH_SIZE);
            List<UserProfile> batch = userProfileMapper.selectList(wrapper);
            if (batch == null || batch.isEmpty()) {
                break;
            }
            writer.write(toRows(batch, columns), writeSheet);
            lastId = batch.get(batch.size() - 1).getId();
            exported += batch.size();
            updateProgress(taskId, total, exported, "全量导出中，已导出 " + exported + "/" + total);
        }
    }

    private void updateProgress(String taskId, Long totalCount, Long exportedCount, String message) {
        ExportTaskRecord task = getRequiredTask(taskId);
        task.setTotalCount(totalCount == null ? 0L : totalCount);
        task.setExportedCount(exportedCount == null ? 0L : exportedCount);
        task.setMessage(message);
        exportTaskMapper.updateById(task);
    }

    private void markFailed(String taskId, String message, String failReason) {
        ExportTaskRecord task = getRequiredTask(taskId);
        task.setStatus(ExportTaskStatus.FAILED.name());
        task.setMessage(message + (StringUtils.hasText(failReason) ? ": " + failReason : ""));
        task.setFailReason(failReason);
        task.setFinishedAt(LocalDateTime.now());
        exportTaskMapper.updateById(task);
    }

    private void ensureActiveTaskCapacity() {
        Long activeCount = exportTaskMapper.selectCount(new LambdaQueryWrapper<ExportTaskRecord>()
                .eq(ExportTaskRecord::getBusinessType, BUSINESS_TYPE)
                .in(ExportTaskRecord::getStatus, ExportTaskStatus.WAITING.name(), ExportTaskStatus.RUNNING.name()));
        if (activeCount != null && activeCount >= MAX_ACTIVE_TASKS) {
            throw new BusinessException("导出队列已满，请稍后再试");
        }
    }

    private void recoverInterruptedTasks() {
        List<ExportTaskRecord> tasks = exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTaskRecord>()
                .eq(ExportTaskRecord::getBusinessType, BUSINESS_TYPE)
                .in(ExportTaskRecord::getStatus, ExportTaskStatus.WAITING.name(), ExportTaskStatus.RUNNING.name()));
        for (ExportTaskRecord task : tasks) {
            task.setStatus(ExportTaskStatus.FAILED.name());
            task.setMessage("服务重启，任务已中断，请重新提交");
            task.setFailReason("服务重启导致任务中断");
            task.setFinishedAt(LocalDateTime.now());
            exportTaskMapper.updateById(task);
        }
    }

    private void updateExpired(ExportTaskRecord task) {
        try {
            if (StringUtils.hasText(task.getFilePath())) {
                Files.deleteIfExists(Paths.get(task.getFilePath()));
            }
        } catch (Exception ex) {
            log.warn("删除过期文件失败, taskId={}", task.getTaskId(), ex);
        }
        task.setStatus(ExportTaskStatus.EXPIRED.name());
        task.setMessage("文件已过期");
        task.setFilePath(null);
        exportTaskMapper.updateById(task);
    }

    private LambdaQueryWrapper<UserProfile> buildQueryWrapper(String name) {
        return new LambdaQueryWrapper<UserProfile>()
                .like(StringUtils.hasText(name), UserProfile::getName, name);
    }

    private List<List<String>> buildHead(List<UserProfileExportColumn> columns) {
        return columns.stream().map(item -> List.of(item.getHead())).collect(Collectors.toList());
    }

    private List<List<Object>> toRows(List<UserProfile> users, List<UserProfileExportColumn> columns) {
        List<List<Object>> rows = new ArrayList<>();
        for (UserProfile user : users) {
            List<Object> row = new ArrayList<>();
            for (UserProfileExportColumn column : columns) {
                row.add(column.getExtractor().apply(user));
            }
            rows.add(row);
        }
        return rows;
    }

    private ExportTaskResponse toResponse(ExportTaskRecord task) {
        String downloadUrl = ExportTaskStatus.SUCCESS.name().equals(task.getStatus())
                ? "/userProfile/export/tasks/" + task.getTaskId() + "/download"
                : null;
        return ExportTaskResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus())
                .message(task.getMessage())
                .fileName(task.getFileName())
                .downloadUrl(downloadUrl)
                .totalCount(task.getTotalCount())
                .exportedCount(task.getExportedCount())
                .build();
    }

    private void validateRequest(UserProfileExportRequest request) {
        boolean exportAll = isExportAll(request);
        if (!exportAll) {
            if (request.getPageNo() == null || request.getPageSize() == null) {
                throw new BusinessException("分页参数不能为空");
            }
            if (request.getPageNo() <= 0 || request.getPageSize() <= 0) {
                throw new BusinessException("分页导出时，pageNo/pageSize 必须大于0；导出全部请传 -1/-1");
            }
        }
        if (request.getPageNo() == null || request.getPageSize() == null) {
            throw new BusinessException("pageNo/pageSize 不能为空");
        }
        if (!(exportAll || (request.getPageNo() > 0 && request.getPageSize() > 0))) {
            throw new BusinessException("仅支持正常分页或 pageNo=-1 且 pageSize=-1 的全量导出");
        }
        if (request.getFields().size() > MAX_EXPORT_COLUMNS) {
            throw new BusinessException("导出字段过多，最多支持 " + MAX_EXPORT_COLUMNS + " 列");
        }
        parseColumns(request.getFields());
    }

    private List<UserProfileExportColumn> parseColumns(List<String> fields) {
        List<UserProfileExportColumn> columns = new ArrayList<>();
        for (String field : fields) {
            try {
                columns.add(UserProfileExportColumn.fromCode(field));
            } catch (IllegalArgumentException ex) {
                throw new BusinessException(ex.getMessage());
            }
        }
        return columns;
    }

    private boolean isExportAll(UserProfileExportRequest request) {
        return request.getPageNo() == -1 && request.getPageSize() == -1;
    }

    private ExportTaskRecord getRequiredTask(String taskId) {
        ExportTaskRecord task = exportTaskMapper.selectOne(new LambdaQueryWrapper<ExportTaskRecord>()
                .eq(ExportTaskRecord::getTaskId, taskId)
                .last("limit 1"));
        if (task == null) {
            throw new BusinessException("任务不存在: " + taskId);
        }
        return task;
    }

    private String writeJson(List<String> fields) {
        try {
            return objectMapper.writeValueAsString(fields);
        } catch (JsonProcessingException e) {
            throw new BusinessException("序列化导出字段失败: " + e.getMessage());
        }
    }

    @SuppressWarnings("unused")
    private List<String> readFields(String fieldsJson) {
        try {
            return objectMapper.readValue(fieldsJson, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            throw new BusinessException("反序列化导出字段失败: " + e.getMessage());
        }
    }

    private void ensureDir() {
        try {
            Files.createDirectories(exportDir);
        } catch (Exception ex) {
            throw new BusinessException("创建导出目录失败: " + ex.getMessage());
        }
    }
}
