package cc.ivera.service;

import cc.ivera.entity.FlowInfo;
import cc.ivera.entity.User;
import cc.ivera.mapper.FlowInfoMapper;
import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FlowService {
    @Autowired
    private ActFlowCommService actFlowCommService;
    @Autowired
    private FlowInfoMapper flowInfoMapper;
    @Autowired
    private UserService userService;
    @Autowired
    private SiteMessageService siteMessageService;

    /**
     * 查询用户任务
     *
     * @param userId
     * @return
     */
    public List<Map<String, Object>> findUserTask(Long userId) {
        List<Map<String, Object>> list = actFlowCommService.myTaskList(userId.toString());
        return list;
    }

    /**
     * 完成任务
     *
     * @param userId
     */
    public void completeTask(String taskId, Long userId) {
        actFlowCommService.completeProcess("同意", taskId, userId.toString());
    }

    /**
     * 查询所有流程
     *
     * @return
     */
    public List<FlowInfo> findAllFlow() {
        List<FlowInfo> flowInfos = flowInfoMapper.selectFlowList();
        return flowInfos;
    }

    /**
     * 查询单个流程
     *
     * @param id
     * @return
     */
    public FlowInfo findOneFlow(Long id) {
        return flowInfoMapper.selectOneFlow(id);
    }

    /**
     * 更新部署状态
     *
     * @param id
     * @return
     */
    public int updateDeployState(Long id) {
        return flowInfoMapper.updateFlowDeployState(id);
    }

    /**
     * 任务创建事件
     *
     * @param delegateTask
     */
    public void createTaskEvent(DelegateTask delegateTask) {
        log.info("delegateTask=={}", delegateTask);
//        负责人
        String assignee = delegateTask.getAssignee();
//        获取当前登录用户
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userService.findOneUserByName(username);
        String userId = user.getId().toString();
//        任务id
        String taskId = delegateTask.getId();
        if (!assignee.equals(userId)) {
            int type = 1;
            siteMessageService.sendMsg(Long.valueOf(assignee), taskId, type, 1);
        }

    }

    /**
     * 查询任务详细信息
     *
     * @param userId
     * @return
     */
    public List<Map<String, Object>> findTaskInfo(Long userId) {
        List<Map<String, Object>> list = actFlowCommService.myTaskInfoList(userId.toString());
        return list;
    }
}
