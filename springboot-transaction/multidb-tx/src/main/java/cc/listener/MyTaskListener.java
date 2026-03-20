package cc.ivera.listener;

import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

/**
 * 监听器 在TestListener在的方法中被调用
 *
 * String EVENTNAME_CREATE = "create";
 *   String EVENTNAME_ASSIGNMENT = "assignment";
 *   String EVENTNAME_COMPLETE = "complete";
 *   String EVENTNAME_DELETE = "delete";
 */
public class MyTaskListener implements TaskListener {
    /**
     * 指定负责人
     *
     * @param delegateTask
     */
    @Override
    public void notify(DelegateTask delegateTask) {
        //判断当前的任务 是 创建申请 并且  是 create事件
        //本实例的含义是指定填写请假单的人为张三
        if ("创建申请".equals(delegateTask.getName()) &&
                "create".equals(delegateTask.getEventName())) {
            delegateTask.setAssignee("张三");
        }

    }
}
