package com.demo.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.order.domain.JobCronConfig;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface JobCronConfigMapper extends BaseMapper<JobCronConfig> {
    @Select("SELECT * FROM job_cron_config WHERE job_type=#{jobType} LIMIT 1")
    JobCronConfig selectByJobType(@Param("jobType") String jobType);
}
