package cc.ivera.test;


import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;


//出差申请大于等于3天需要由项目经理审批，小于3天由技术经理审批，出差申请必须经过人事经理审批
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:activiti-spring.xml")
public class ActivitiGatewayInclusive8 {

    /**
     * Service providing access to the repository of process definitions and deployments.
     */
    @Autowired
    private RepositoryService repositoryService;

    /**
     * Starts a new process instance in the latest version of the process definition with the given key.
     */
    @Autowired
    private RuntimeService runtimeService;


    @Autowired
    private TaskService taskService;


    @Autowired
    private HistoryService historyService;

    /**
     * 部署流程定义
     */
    @Test
    public void testDeployment() {
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("bpmn/evection-inclusive.bpmn")//添加bpmn资源
                .name("出差申请流程-包含网关")
                .deploy();

        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());
    }

    /**
     * 启动流程实例,设置流程变量的值 一个流程办绑定一个key
     * act_hi_procinst
     */
    @Test
    public void startProcess() {
        String key = "inclusive";
        Map<String, Object> map = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(4d);
        //global变量
        map.put("evection", evection);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, map);
        System.out.println("流程实例名称=" + processInstance.getName());
        System.out.println("流程定义id==" + processInstance.getProcessDefinitionId());
    }


    /**
     * tom填写出差申请单
     */
    @Test
    public void completTask0() {

        String key = "inclusive";
        String assingee = "tom";
        Map<String, Object> map = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(3d);
        map.put("evection", evection);
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .list().get(0);
        if (task != null) {
            taskService.complete(task.getId(), map);
            System.out.println(task.getId());
        }

    }

    //人事经理审批 项目经理审批
    @Test
    public void completTask1() {
        String key = "inclusive";
        String assingee = "jack";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            taskService.complete(task.getId());
            System.out.println(task.getId());
            System.out.println("干活");
        } else {
            System.out.println("摸鱼一下");
        }

    }


    /**
     * 总经理审批
     */
    @Test
    public void completTask2() {

        String key = "inclusive";
        String assingee = "rose";

        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            taskService.complete(task.getId());
            System.out.println(task.getId());
            System.out.println("干活");
        } else {
            System.out.println("摸鱼一下");
        }

    }
}
