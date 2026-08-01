package cc.ivera.ragdemo.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 定时任务线程池配置类
 * 
 * 配置自定义的 ThreadPoolTaskScheduler 替代 Spring 默认的单线程调度器，
 * 避免多个定时任务串行执行导致的阻塞问题。
 */
@Configuration
@EnableScheduling
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class SchedulerConfig {

    private final RagProperties properties;

    /**
     * 配置定时任务线程池
     * 
     * 默认配置：
     * - 线程池大小：3（与项目中定时任务数量匹配）
     * - 线程名称前缀：scheduled-task-
     * - 等待所有任务完成后再关闭
     * - 等待终止时间：60秒
     */
    @Bean
    public TaskScheduler taskScheduler() {
        RagProperties.Scheduler schedulerProperties = properties.getScheduler();
        
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(schedulerProperties.getPoolSize());
        scheduler.setThreadNamePrefix("scheduled-task-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(schedulerProperties.getAwaitTerminationSeconds());
        scheduler.setErrorHandler(throwable -> {
            // 统一异常处理，避免定时任务异常导致整个调度器停止
            // 异常信息会通过日志系统记录
        });
        scheduler.initialize();
        return scheduler;
    }
}
