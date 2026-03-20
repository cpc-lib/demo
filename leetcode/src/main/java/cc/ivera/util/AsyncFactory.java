package cc.ivera.util;

import lombok.SneakyThrows;
import cc.ivera.model.pojo.Job;

import java.util.TimerTask;

public class AsyncFactory {
    public static TimerTask execute(Job job) {
        return new TimerTask() {
            @SneakyThrows
            @Override
            public void run() {
                // 远程查询操作地点
                String beanName = job.getBeanName();
                String methodName = job.getMethodName();
                Object[] param = job.getParam();
                SpringUtil.springInvokeMethod(beanName, methodName, param);
            }
        };
    }
}
