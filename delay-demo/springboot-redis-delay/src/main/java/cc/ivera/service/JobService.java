package cc.ivera.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import cc.ivera.bean.DelayJob;
import cc.ivera.bean.Job;
import cc.ivera.constants.JobStatus;
import cc.ivera.container.DelayBucket;
import cc.ivera.container.JobPool;
import cc.ivera.container.ReadyQueue;

/**
 * @author daify
 * @date 2019-07-28
 */
@Component
public class JobService {
    
    @Autowired
    private DelayBucket delayBucket;
    
    @Autowired
    private ReadyQueue readyQueue;

    @Autowired
    private JobPool jobPool;


    public DelayJob addDefJob(Job job) {
        job.setStatus(JobStatus.DELAY);
        jobPool.addJob(job);
        DelayJob delayJob = new DelayJob(job);
        delayBucket.addDelayJob(delayJob);
        return delayJob;
    }

    /**
     * 获取
     * @return
     */
    public Job getProcessJob(String topic) {
        // 拿到任务
        DelayJob delayJob = readyQueue.popJob(topic);
        if (delayJob == null || delayJob.getJodId() == 0L) {
            return new Job();
        }
        Job job = jobPool.getJob(delayJob.getJodId());
        // 元数据已经删除，则取下一个
        if (job == null) {
            job = getProcessJob(topic);
        } else {
            job.setStatus(JobStatus.RESERVED);
            delayJob.setDelayDate(System.currentTimeMillis() + job.getTtrTime());

            jobPool.addJob(job);
            delayBucket.addDelayJob(delayJob);  
        }
        return job;
    }

    /**
     * 完成一个执行的任务
     * @param jobId
     * @return
     */
    public void finishJob(Long jobId) {
        jobPool.removeDelayJob(jobId);
    }

    /**
     * 完成一个执行的任务
     * @param jobId
     * @return
     */
    public void deleteJob(Long jobId) {
        jobPool.removeDelayJob(jobId);
    }
    
}
