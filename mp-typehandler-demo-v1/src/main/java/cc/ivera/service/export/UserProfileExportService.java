package cc.ivera.service.export;

import cc.ivera.common.BusinessException;
import cc.ivera.dto.ExportTaskResponse;
import cc.ivera.dto.UserProfileExportRequest;
import cc.ivera.entity.UserProfile;
import cc.ivera.enums.ExportTaskStatus;
import cc.ivera.excel.UserProfileExportColumn;
import cc.ivera.mapper.UserProfileMapper;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserProfileExportService {

    private static final int EXPORT_BATCH_SIZE = 500;
    private static final int MAX_EXPORT_COLUMNS = 20;
    private static final long FILE_EXPIRE_HOURS = 24;

    private final UserProfileMapper userProfileMapper;
    private final Executor exportTaskExecutor;

    public UserProfileExportService(UserProfileMapper userProfileMapper,
                                    @Qualifier("exportTaskExecutor") Executor exportTaskExecutor) {
        this.userProfileMapper = userProfileMapper;
        this.exportTaskExecutor = exportTaskExecutor;
    }

    private final Map<String, ExportTaskInfo> taskStore = new ConcurrentHashMap<>();
    private final Path exportDir = Paths.get(System.getProperty("java.io.tmpdir"), "mp-typehandler-demo-exports");

    public ExportTaskResponse submit(UserProfileExportRequest request) {
        validateRequest(request);
        ensureDir();

        String taskId = UUID.randomUUID().toString().replace("-", "");
        ExportTaskInfo task = new ExportTaskInfo();
        task.setTaskId(taskId);
        task.setStatus(ExportTaskStatus.WAITING);
        task.setMessage("任务已提交，等待执行");
        task.setRequest(request);
        task.setCreateTime(LocalDateTime.now());
        task.setExpireTime(LocalDateTime.now().plusHours(FILE_EXPIRE_HOURS));
        taskStore.put(taskId, task);

        try {
            exportTaskExecutor.execute(() -> doExport(task));
        } catch (RejectedExecutionException ex) {
            taskStore.remove(taskId);
            throw new BusinessException("导出队列已满，请稍后再试");
        }
        return toResponse(task);
    }

    public ExportTaskResponse getTask(String taskId) {
        return toResponse(getRequiredTask(taskId));
    }

    public Resource getExportFile(String taskId) {
        ExportTaskInfo task = getRequiredTask(taskId);
        if (task.getStatus() != ExportTaskStatus.SUCCESS) {
            throw new BusinessException("任务未完成，暂不可下载");
        }
        if (task.getExpireTime() != null && task.getExpireTime().isBefore(LocalDateTime.now())) {
            task.setStatus(ExportTaskStatus.EXPIRED);
            task.setMessage("文件已过期");
            throw new BusinessException("文件已过期，请重新导出");
        }
        if (task.getFilePath() == null || !Files.exists(task.getFilePath())) {
            throw new BusinessException("导出文件不存在，请重新导出");
        }
        return new FileSystemResource(task.getFilePath());
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 * * * ?")
    public void clearExpiredFiles() {
        LocalDateTime now = LocalDateTime.now();
        taskStore.values().forEach(task -> {
            if (task.getExpireTime() != null && task.getExpireTime().isBefore(now)) {
                try {
                    if (task.getFilePath() != null) {
                        Files.deleteIfExists(task.getFilePath());
                    }
                } catch (Exception ex) {
                    log.warn("删除过期文件失败, taskId={}", task.getTaskId(), ex);
                }
                task.setStatus(ExportTaskStatus.EXPIRED);
                task.setMessage("文件已过期");
            }
        });
    }

    private void doExport(ExportTaskInfo task) {
        task.setStatus(ExportTaskStatus.RUNNING);
        task.setMessage("任务执行中");
        UserProfileExportRequest request = task.getRequest();
        List<UserProfileExportColumn> columns = parseColumns(request.getFields());

        String fileName = "user-profile-export-" + task.getTaskId() + ".xlsx";
        Path filePath = exportDir.resolve(fileName);
        task.setFileName(fileName);
        task.setFilePath(filePath);

        try (ExcelWriter writer = EasyExcel.write(filePath.toFile())
                .head(buildHead(columns))
                .autoCloseStream(true)
                .build()) {
            WriteSheet writeSheet = EasyExcel.writerSheet("用户数据").build();

            if (isExportAll(request)) {
                exportAll(writer, writeSheet, task, columns);
            } else {
                exportOnePage(writer, writeSheet, task, columns);
            }

            task.setStatus(ExportTaskStatus.SUCCESS);
            task.setMessage("导出成功");
        } catch (Exception ex) {
            log.error("导出失败, taskId={}", task.getTaskId(), ex);
            task.setStatus(ExportTaskStatus.FAILED);
            task.setMessage("导出失败: " + ex.getMessage());
            try {
                if (filePath != null) {
                    Files.deleteIfExists(filePath);
                }
            } catch (Exception deleteEx) {
                log.warn("删除失败文件失败, taskId={}", task.getTaskId(), deleteEx);
            }
        }
    }

    private void exportOnePage(ExcelWriter writer, WriteSheet writeSheet, ExportTaskInfo task,
                               List<UserProfileExportColumn> columns) {
        UserProfileExportRequest request = task.getRequest();
        Page<UserProfile> page = new Page<>(request.getPageNo(), request.getPageSize());
        LambdaQueryWrapper<UserProfile> wrapper = buildQueryWrapper(request.getName())
                .orderByDesc(UserProfile::getId);
        Page<UserProfile> result = userProfileMapper.selectPage(page, wrapper);
        task.setTotalCount(result.getTotal());
        List<List<Object>> rows = toRows(result.getRecords(), columns);
        writer.write(rows, writeSheet);
        task.getExportedCount().set(result.getRecords().size());
        task.setMessage("分页导出完成");
    }

    private void exportAll(ExcelWriter writer, WriteSheet writeSheet, ExportTaskInfo task,
                           List<UserProfileExportColumn> columns) {
        UserProfileExportRequest request = task.getRequest();
        long total = userProfileMapper.selectCount(buildQueryWrapper(request.getName()));
        task.setTotalCount(total);

        Long lastId = 0L;
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
            task.getExportedCount().addAndGet(batch.size());
            task.setMessage("全量导出中，已导出 " + task.getExportedCount().get() + "/" + task.getTotalCount());
        }
    }

    private LambdaQueryWrapper<UserProfile> buildQueryWrapper(String name) {
        return new LambdaQueryWrapper<UserProfile>()
                .like(StringUtils.hasText(name), UserProfile::getName, name);
    }

    private List<List<String>> buildHead(List<UserProfileExportColumn> columns) {
        return columns.stream()
                .map(item -> List.of(item.getHead()))
                .collect(Collectors.toList());
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

    private ExportTaskResponse toResponse(ExportTaskInfo task) {
        String downloadUrl = task.getStatus() == ExportTaskStatus.SUCCESS
                ? "/userProfile/export/tasks/" + task.getTaskId() + "/download"
                : null;
        return ExportTaskResponse.builder()
                .taskId(task.getTaskId())
                .status(task.getStatus().name())
                .message(task.getMessage())
                .fileName(task.getFileName())
                .downloadUrl(downloadUrl)
                .totalCount(task.getTotalCount())
                .exportedCount(task.getExportedCount().get())
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

    private ExportTaskInfo getRequiredTask(String taskId) {
        ExportTaskInfo task = taskStore.get(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在: " + taskId);
        }
        return task;
    }

    private void ensureDir() {
        try {
            Files.createDirectories(exportDir);
        } catch (Exception ex) {
            throw new BusinessException("创建导出目录失败: " + ex.getMessage());
        }
    }
}
