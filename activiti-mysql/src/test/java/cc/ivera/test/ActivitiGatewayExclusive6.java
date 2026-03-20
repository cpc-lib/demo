package cc.ivera.test;

import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 排他网关，用来在流程中实现决策。 当流程执行到这个网关，所有分支都会判断条件是否为true，如果为true则执行该分支，
 *
 * **注意**：排他网关只会选择一个为true的分支执行。如果有两个分支条件都为true，排他网关会选择id值较小的一条分支去执行。
 *
 * 为什么要用排他网关？
 *
 * 不用排他网关也可以实现分支，如：在连线的condition条件上设置分支条件。
 *
 * 在连线设置condition条件的缺点：如果条件都不满足，流程就结束了(是异常结束)。
 */

public class ActivitiGatewayExclusive6 {
    /**
     * 部署流程定义
     */
    @Test
    public void testDeployment(){
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、得到RepositoryService实例
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、使用RepositoryService进行部署
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("bpmn/evection-exclusive.bpmn") // 添加bpmn资源
//                .addClasspathResource("bpmn/evection.png")  // 添加png资源
                .name("出差申请流程-排他网关")
                .deploy();
//        4、输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());
    }

    /**
     * 启动流程实例,设置流程变量的值
     */
    @Test
    public void startProcess(){
//        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        流程定义key
        String key = "exclusive";
//       创建变量集合
        Map<String, Object> map = new HashMap<>();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置出差天数
        evection.setNum(5d);
//      定义流程变量，把出差pojo对象放入map
        map.put("evection",evection);
//        启动流程实例，并设置流程变量的值（把map传入）
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, map);
//      输出
        System.out.println("流程实例名称="+processInstance.getName());
        System.out.println("流程定义id=="+processInstance.getProcessDefinitionId());
        System.out.println("流程实例id=="+processInstance.getProcessInstanceId());
    }

//select * from act_hi_taskinst where PROC_INST_ID_='130001'
    //ACT_RU_TASK
    @Test
    public void completeTask0(){
        //流程定义的Key
        String key = "exclusive";
//        任务负责人
        String assingee = "tom";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println("完成了清单的提交"+task.getId());
        }
    }


    @Test
    public void completTask1(){
//        流程定义的Key
        String key = "exclusive";
//        任务负责人
        String assingee = "jerry";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println(task.getId());
        }

    }

    //总经理审批
    @Test
    public void completTask2(){
//        流程定义的Key
        String key = "exclusive";
//        任务负责人
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
        if(task != null){
            //     根据任务id来   完成任务
            //taskService.complete(task.getId());
            //删除任务
            taskService.deleteTask(task.getId());
            System.out.println(task.getId());
        }

    }

    //财务审批
    @Test
    public void completTask3(){
//        流程定义的Key
        String key = "exclusive";
//        任务负责人
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
        if(task != null){
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println(task.getId());
        }

    }
}
