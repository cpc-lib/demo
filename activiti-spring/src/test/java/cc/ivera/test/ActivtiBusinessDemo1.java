package cc.ivera.test;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:activiti-spring.xml")
public class ActivtiBusinessDemo1 {

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

    /**
     * 流程部署
     */
    @Test
    public void testDeployment(){
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程")
                .addClasspathResource("bpmn/evection.bpmn")
                .deploy();
        System.out.println("流程部署id="+deploy.getId());
        System.out.println("流程部署名字="+deploy.getName());
    }



    /**
     * 添加业务key 到Activiti的表
     */
    @Test
    public void addBusinessKey(){
        //启动流程的过程中，添加businesskey
        //第一个参数：流程定义的key
        //第二个参数：businessKey，存出差申请单的id，就是1001
        ProcessInstance instance = runtimeService.
                startProcessInstanceByKey("myEvection", "1001");
        System.out.println("businessKey=="+instance.getBusinessKey());

    }

    /**
     * 根据定义流程定义的名称控制流程
     * 全部流程实例
     * 挂起激活
     * 挂起,流程实例无法进行
     * suspend 暂停
     */
    @Test
    public void suspendAllProcessInstance(){

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("myEvection")
                .list().get(0);
        boolean suspended = processDefinition.isSuspended();

        String definitionId = processDefinition.getId();

        if(suspended){
            //如果是挂起，可以执行激活的操作,参数1：流程定义id 参数2：是否激活，参数3：激活时间
            repositoryService.activateProcessDefinitionById(definitionId,
                    true,
                    null);
            System.out.println("流程定义id:"+definitionId+"，已激活");
        }else {
            //如果是激活状态，改为挂起状态,参数1：流程定义id 参数2：是否暂停 参数3 ：暂停的时间
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
        ProcessInstance instance = runtimeService.createProcessInstanceQuery()
                .processInstanceId("27501")
                .singleResult();
        //得到当前流程实例的暂停状态,true-已暂停  false -激活
        boolean suspended = instance.isSuspended();
        String instanceId = instance.getId();
        //判断是否已经暂停，如果已经暂停，就执行激活操作
        if(suspended){
            //如果已经暂停，就执行激活
            runtimeService.activateProcessInstanceById(instanceId);
            System.out.println("流程实例id:"+instanceId+"已经激活");
        }else {
            //如果是激活状态，就执行暂停操作
            runtimeService.suspendProcessInstanceById(instanceId);
            System.out.println("流程实例id:"+instanceId+"已经暂停");
        }
    }

    /**
     * 完成个人任务
     * act_ru_task
     * act_hi_procinst
     */
    @Test
    public void completTask(){
        Task task = taskService.createTaskQuery()
                .processInstanceId("35001")
                .taskAssignee("zhangsan")
                .singleResult();
        System.out.println("流程实例id=="+task.getProcessInstanceId());
        System.out.println("流程任务id=="+task.getId());
        System.out.println("负责人=="+task.getAssignee());
        System.out.println("任务名称=="+task.getName());
        taskService.complete(task.getId());
    }


    //需求：
    //在 activiti 实际应用时，查询待办任务可能要显示出业务系统的一些相关信息。
    //比如：查询待审批出差任务列表需要将出差单的日期、 出差天数等信息显示出来。
    //出差天数等信息在业务系统中存在，而并没有在 activiti 数据库中存在，所以是无法通过 activiti 的 api 查询到出差天数等信息。
    //实现：
    //在查询待办任务时，通过 businessKey（业务标识 ）关联查询业务系统的出差单表，查询出出差天数等信息。
    @Test
    public void findProcessInstance(){
        Task task = taskService.createTaskQuery()
                .processDefinitionKey("myEvection")
                .taskAssignee("zhangsan")
                .singleResult();
        String processInstanceId = task.getProcessInstanceId();
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        String businessKey = processInstance.getBusinessKey();
        System.out.println("businessKey=="+businessKey);
    }
}
