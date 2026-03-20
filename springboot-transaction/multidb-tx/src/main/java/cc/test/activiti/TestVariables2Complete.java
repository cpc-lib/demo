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
 * 测试任务完成时添加流程变量
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={Application.class})
public class TestVariables2Complete {

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
                .name("出差申请流程-variables-complete")
                .addClasspathResource("processes/evection-global.bpmn")
                .deploy();
        System.out.println("流程部署id="+deploy.getId());
        System.out.println("流程部署名字="+deploy.getName());
    }

    /**
     * 启动流程 的时候设置流程变量
     * 设置流程变量num
     * 设置任务负责人
     */
    @Test
    public void testStartProcess(){
        String key = "myEvection2";
        Map<String,Object> variables = new HashMap<>();
        variables.put("assignee0","李四3");
        variables.put("assignee1","王经理3");
        variables.put("assignee2","杨总经理3");
        variables.put("assignee3","张财务3");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key, variables);
        System.out.println(processInstance.getId());
    }

    /**
     * 完成个人任务
     */
    @Test
    public void completTask(){
        String key = "myEvection2";
        String assingee = "王经理3";
        Evection evection = new Evection();
        evection.setNum(2d);
        Map<String,Object> map = new HashMap<>();
        map.put("evection",evection);
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            taskService.complete(task.getId(),map);
            System.out.println(task.getId()+"----任务已完成");
        }

    }
}
