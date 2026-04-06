package cc.ivera.job;

import cc.ivera.service.export.UserProfileExportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExportTaskCleanupJob {

    private final UserProfileExportService userProfileExportService;

    /**
     * 定时清理已过期导出文件
     */
    //@Scheduled(cron = "${export.cleanup.cron:0 0 * * * ?}")
    @Scheduled(cron = "${export.cleanup.cron:*/5 * * * * ?}")
    public void cleanupExpiredFiles() {
        System.out.println("!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
        log.info("开始执行导出文件过期清理任务");
        try {
            int count = userProfileExportService.cleanupExpiredTasks();
            log.info("导出文件过期清理任务完成, cleanedCount={}", count);
        } catch (Exception e) {
            log.error("导出文件过期清理任务执行失败", e);
        }
    }
}
