package cc.ivera.test.activiti;

import cc.ivera.Application;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

/**
 * 测试 使用url 设置负责人
 * TaskListener
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={Application.class})
public class TestListener3 {

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
                .name("测试监听器")
                .addClasspathResource("processes/demo-listen.bpmn")
                .deploy();
        System.out.println("流程部署id="+deploy.getId());
        System.out.println("流程部署名字="+deploy.getName());
    }


    @Test
    public void deleteDeployment() {
        String deploymentId = "30001";
        repositoryService.deleteDeployment(deploymentId, true);
    }

    //创建时调用
    @Test
    public void startDemoListener(){
        System.out.println(runtimeService.startProcessInstanceByKey("testListener").getProcessInstanceId());

    }


    @Test
    public void findPersonalTaskList() {
        //流程定义key
        String processDefinitionKey = "testListener";
        //任务负责人
        String assignee = "张三";

        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
                .includeProcessVariables()
                .taskAssignee(assignee)
                .list();
        for (Task task : taskList) {
            System.out.println("----------------------------");
            System.out.println("流程实例id： " + task.getProcessInstanceId());
            System.out.println("任务id： " + task.getId());
            System.out.println("任务负责人： " + task.getAssignee());
            System.out.println("任务名称： " + task.getName());
        }
    }



}
