package cc.ivera.test;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;

public class ActivtiBusinessDemo1 {


    /**
     * 流程部署
     */
    @Test
    public void testDeployment(){
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取RepositoryServcie
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、使用service进行流程的部署，定义一个流程的名字，把bpmn和png部署到数据中
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程")
                .addClasspathResource("bpmn/evection.bpmn")
                .addClasspathResource("bpmn/evection.png")
                .deploy();
//        4、输出部署信息
        System.out.println("流程部署id="+deploy.getId());
        System.out.println("流程部署名字="+deploy.getName());
    }



    /**
     * 添加业务key 到Activiti的表
     */
    @Test
    public void addBusinessKey(){
//        1、获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取RuntimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        3、启动流程的过程中，添加businesskey
//           第一个参数：流程定义的key
//           第二个参数：businessKey，存出差申请单的id，就是1001
        ProcessInstance instance = runtimeService.
                startProcessInstanceByKey("myEvection", "1001");
//        4、输出
        System.out.println("businessKey=="+instance.getBusinessKey());

    }

    /**
     * 全部流程实例的 挂起和 激活
     * suspend 暂停
     */
    @Test
    public void suspendAllProcessInstance(){
//        1、获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取Repositoryservice
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、查询流程定义,获取流程定义的查询对象
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("myEvection")
                .singleResult();
//        4、获取当前流程定义的实例是否都是挂起状态
        boolean suspended = processDefinition.isSuspended();
//        5、获取流程定义的id
        String definitionId = processDefinition.getId();
//        6、如果是挂起状态，改为激活状态
        if(suspended){
//            如果是挂起，可以执行激活的操作,参数1：流程定义id 参数2：是否激活，参数3：激活时间
            repositoryService.activateProcessDefinitionById(definitionId,
                    true,
                    null);
            System.out.println("流程定义id:"+definitionId+"，已激活");
        }else {
//        7、如果是激活状态，改为挂起状态,参数1：流程定义id 参数2：是否暂停 参数3 ：暂停的时间
            repositoryService.suspendProcessDefinitionById(definitionId,
                    true,
                    null);
            System.out.println("流程定义id:"+definitionId+"，已挂起");
        }
    }

    /**
     * 挂起、激活单个流程实例
     * act_ru_execution
     */
    @Test
    public void suspendSingleProcessInstance(){
//        1、获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、RuntimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        3、通过RuntimeService获取流程实例对象
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId("27501")
                .singleResult();
//        4、得到当前流程实例的暂停状态,true-已暂停  false -激活
        boolean suspended = instance.isSuspended();
//        5、获取流程实例id
        String instanceId = instance.getId();
//        6、判断是否已经暂停，如果已经暂停，就执行激活操作
        if(suspended){
//            如果已经暂停，就执行激活
            runtimeService.activateProcessInstanceById(instanceId);
            System.out.println("流程实例id:"+instanceId+"已经激活");
        }else {
//        7、如果是激活状态，就执行暂停操作
            runtimeService.suspendProcessInstanceById(instanceId);
            System.out.println("流程实例id:"+instanceId+"已经暂停");
        }
    }

    /**
     * 完成个人任务
     */
    @Test
    public void completTask(){
//        1、获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        3、使用taskservice获取任务,参数 流程实例的id，负责人
        Task task = taskService.createTaskQuery()
                .processInstanceId("27501")
                .taskAssignee("zhangsan")
                .singleResult();
        System.out.println("流程实例id=="+task.getProcessInstanceId());
        System.out.println("流程任务id=="+task.getId());
        System.out.println("负责人=="+task.getAssignee());
        System.out.println("任务名称=="+task.getName());
//        4、根据任务的id完成任务
        taskService.complete(task.getId());
    }


    //需求：
    //在 activiti 实际应用时，查询待办任务可能要显示出业务系统的一些相关信息。
    //
    //比如：查询待审批出差任务列表需要将出差单的日期、 出差天数等信息显示出来。
    //
    //出差天数等信息在业务系统中存在，而并没有在 activiti 数据库中存在，所以是无法通过 activiti 的 api 查询到出差天数等信息。
    //实现：
    //在查询待办任务时，通过 businessKey（业务标识 ）关联查询业务系统的出差单表，查询出出差天数等信息。
    @Test
    public void findProcessInstance(){
//        获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        获取RuntimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        查询流程定义的对象
        Task task = taskService.createTaskQuery()
                .processDefinitionKey("myEvection1")
                .taskAssignee("张三")
                .singleResult();
//        使用task对象获取实例id
        String processInstanceId = task.getProcessInstanceId();
//          使用实例id，获取流程实例对象
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
//        使用processInstance，得到 businessKey
        String businessKey = processInstance.getBusinessKey();

        System.out.println("businessKey=="+businessKey);

    }
}
