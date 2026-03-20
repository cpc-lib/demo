package cc.ivera.test.activiti;

import cc.ivera.Application;
import cc.ivera.domain.Evection;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
@RunWith(SpringRunner.class)
@SpringBootTest(classes={Application.class})
public class ActivitiGatewayExclusive6 {

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
     * 部署流程定义
     */
    @Test
    public void testDeployment(){
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/evection-exclusive.bpmn")//添加bpmn资源
                .name("出差申请流程-排他网关")
                .deploy();

        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());
    }


    /**
     * 删除流程部署信息
     * `act_ge_bytearray`
     * `act_re_deployment`
     * `act_re_procdef`
     * 当前的流程如果并没有完成，想要删除的话需要使用特殊方式，原理就是 级联删除
     */
    @Test
    public void deleteDeployment() {
        String deploymentId = "60001";
        repositoryService.deleteDeployment(deploymentId, true);
    }

    /**
     * 启动流程实例,设置流程变量的值
     */
    @Test
    public void startProcess(){
        String key = "exclusive";
        Map<String, Object> map = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(5d);
        map.put("evection",evection);
        ProcessInstance processInstance = runtimeService
                .startProcessInstanceByKey(key, map);
        System.out.println("流程实例名称="+processInstance.getName());
        System.out.println("流程定义id=="+processInstance.getProcessDefinitionId());
        System.out.println("流程实例id=="+processInstance.getProcessInstanceId());
    }


    /**
     * select * from act_hi_taskinst where PROC_INST_ID_='130001'
     * ACT_RU_TASK
     */
    @Test
    public void completeTask0(){
        String key = "exclusive";
        String assingee = "tom";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            taskService.complete(task.getId());
            System.out.println("完成了清单的提交"+task.getId());
        }
    }


    @Test
    public void completTask1(){
        String key = "exclusive";
        String assingee = "jerry";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            taskService.complete(task.getId());
            System.out.println(task.getId());
        }

    }

    //总经理审批
    @Test
    public void completTask2(){
        String key = "exclusive";
        String assingee = "jack";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            taskService.complete(task.getId());
            //删除任务报错:The task cannot be deleted because is part of a running process
            //taskService.deleteTask(task.getId());
            System.out.println(task.getId());
        }

    }

    /**
     * 财务审批
     */
    @Test
    public void completTask3(){
        String key = "exclusive";
        String assingee = "rose";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            taskService.complete(task.getId());
            System.out.println(task.getId());
        }

    }
}
