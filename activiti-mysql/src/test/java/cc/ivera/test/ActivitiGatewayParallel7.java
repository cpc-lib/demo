package cc.ivera.test;

import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * 并行网关允许将流程分成多条分支，也可以把多条分支汇聚到一起，并行网关的功能是基于进入和外出顺序流的：
 * <p>
 * l  fork分支：
 * <p>
 * 并行后的所有外出顺序流，为每个顺序流都创建一个并发分支。
 * <p>
 * l  join汇聚：
 * <p>
 * 所有到达并行网关，在此等待的进入分支， 直到所有进入顺序流的分支都到达以后， 流程就会通过汇聚网关。
 * <p>
 * 注意，如果同一个并行网关有多个进入和多个外出顺序流， 它就同时具有分支和汇聚功能。 这时，网关会先汇聚所有进入的顺序流，然后再切分成多个并行分支。
 * <p>
 * **与其他网关的主要区别是，并行网关不会解析条件。** **即使顺序流中定义了条件，也会被忽略
 * <p>
 * 技术经理和项目经理是两个execution分支，在act_ru_execution表有两条记录分别是技术经理和项目经理，act_ru_execution还有一条记录表示该流程实例。
 * <p>
 * 待技术经理和项目经理任务全部完成，在汇聚点汇聚，通过parallelGateway并行网关。
 * <p>
 * 并行网关在业务应用中常用于会签任务，会签任务即多个参与者共同办理的任务。
 * <p>
 * ACT_RU_TASK
 */
public class ActivitiGatewayParallel7 {
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
                .addClasspathResource("bpmn/evection-parallel.bpmn") // 添加bpmn资源
                .name("出差申请流程-并行网关")
                .deploy();
//        4、输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());
    }

    /**
     * 启动流程实例,设置流程变量的值
     */
    @Test
    public void startProcess() {
//        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        流程定义key
        String key = "parallel";
//       创建变量集合
        Map<String, Object> map = new HashMap<>();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置出差天数
        evection.setNum(4d);
//      定义流程变量，把出差pojo对象放入map
        map.put("evection", evection);
//        启动流程实例，并设置流程变量的值（把map传入）
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, map);
//      输出
        System.out.println("流程实例名称=" + processInstance.getName());
        System.out.println("流程定义id==" + processInstance.getProcessDefinitionId());
    }

    /**
     * 启动流程实例
     */
    @Test
    public void testStartProcess() {
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        3、根据流程定义Id启动流程
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey("parallel");
//        输出内容
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
        System.out.println("当前活动Id：" + processInstance.getActivityId());
    }


    @Test
    public void setGlobalVariableByExecutionId(){
//    当前流程实例执行 id，通常设置为当前执行的流程实例
        //175002
        String executionId="170002";
//     获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RuntimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置天数
        evection.setNum(3d);
//      通过流程实例 id设置流程变量
        runtimeService.setVariable(executionId, "evection", evection);
//      一次设置多个值
//      runtimeService.setVariables(executionId, variables)
    }

    //根据taskId获取到全局变量
    @Test
    public void getGlobalVariableByTaskId(){
        //当前待办任务id
        String taskId="175007";
//     获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        //通过任务设置流程变量
        Map<String, Object> variables = taskService.getVariables(taskId);
        Evection evection = (Evection)(variables.get("evection"));
        //断言工具类
        assertEquals(evection.getNum(),3d);
        //System.out.println(num);
    }

    //act_ru_execution
    //ACT_RU_TASK
    @Test
    public void completTask() {
//        流程定义的Key
        String key = "parallel";
//        任务负责人
        String assingee = "tom";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .list();
        Task task = list.get(0);
        if (task != null) {
            //     根据任务id来   完成任务
            //taskService.deleteTask(task.getId());
            taskService.complete(task.getId());
        }

    }

    //项目经理或者技术经理审批
    @Test
    public void completTask1() {
//        流程定义的Key
        String key = "parallel";
//        任务负责人（jack/jerry）
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
        }

    }


    @Test
    public void completTask2() {
//        流程定义的Key
        String key = "parallel";
//        任务负责人（jack/jerry）
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
        }

    }
}
