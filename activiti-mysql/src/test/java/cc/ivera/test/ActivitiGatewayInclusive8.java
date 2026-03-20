package cc.ivera.test;

import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

//TODO 测试InclusiveGateway --activiti
//DONE 2023-02-24 22:41 Herbert

//出差申请大于等于3天需要由项目经理审批，小于3天由技术经理审批，出差申请必须经过人事经理审批
public class ActivitiGatewayInclusive8 {
    /**
     * 部署流程定义
     */
    @Test
    public void testDeployment() {
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、得到RepositoryService实例
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、使用RepositoryService进行部署
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("bpmn/evection-inclusive.bpmn") // 添加bpmn资源
//                .addClasspathResource("bpmn/evection.png")  // 添加png资源
                .name("出差申请流程-包含网关")
                .deploy();
//        4、输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());
    }

    /**
     * 启动流程实例,设置流程变量的值 一个流程办绑定一个key
     * act_hi_procinst
     */
    @Test
    public void startProcess() {
//        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        流程定义key
        String key = "inclusive";
//       创建变量集合
        Map<String, Object> map = new HashMap<>();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置默认出差天数为4天
        evection.setNum(4d);
//      定义流程变量，把出差pojo对象放入map
        //global变量
        map.put("evection", evection);
//        启动流程实例，并设置流程变量的值（把map传入）
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, map);
//      输出
        System.out.println("流程实例名称=" + processInstance.getName());

        System.out.println("流程定义id==" + processInstance.getProcessDefinitionId());
    }


    //tom填写出差申请单
    @Test
    public void completTask0() {
//        流程定义的Key
        String key = "inclusive";
//        任务负责人
        String assingee = "tom";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();

        Map<String, Object> map = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(3d);
        map.put("evection", evection);

//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .list().get(0);
        if (task != null) {
            //     根据任务id来   完成任务
            taskService.complete(task.getId(), map);
            System.out.println(task.getId());
        }

    }

    //人事经理审批 项目经理审批
    @Test
    public void completTask1() {
//        流程定义的Key
        String key = "inclusive";
//        任务负责人 miki jack jerry
        String assingee = "jack";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println(task.getId());
            System.out.println("干活");
        } else {
            System.out.println("摸鱼一下");
        }

    }


    //总经理审批
    @Test
    public void completTask2() {
//        流程定义的Key
        String key = "inclusive";
//        任务负责人 miki jack jerry
        String assingee = "rose";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println(task.getId());
            System.out.println("干活");
        } else {
            System.out.println("摸鱼一下");
        }

    }
}
