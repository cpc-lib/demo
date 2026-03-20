package cc.ivera.test;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试 使用url 设置负责人
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:activiti-spring.xml")
public class TestAssigneeUel2 {

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
                .name("出差申请流程-uel")
                .addClasspathResource("bpmn/evection-uel.bpmn")
                .deploy();
        System.out.println("流程部署id="+deploy.getId());
        System.out.println("流程部署名字="+deploy.getName());
    }


    /**
     * 禁止直接删除,直接删除部署，会把运行中的任务进行删除
     */
    @Test
    public void deleteDeployment(){
        String deploymentId = "17501";
        repositoryService.deleteDeployment(deploymentId, true);
    }

    /**
     * act_ru_variable
     * 流程变量作用域是一个流程实例，流程变量使用Map存储，同一个流程实例设置变量map中key相同，后者覆盖前者。
     */
    @Test
    public void startAssigneeUel(){
        //设定assignee的值，用来替换uel表达式
        Map<String,Object> assigneeMap = new HashMap<>();
        assigneeMap.put("assignee0","zhangsan");
        assigneeMap.put("assignee1","lisi");
        assigneeMap.put("assignee2","wangwu");
        assigneeMap.put("assignee3","zhaoliu");
        //启动流程实例
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("myEvection1", assigneeMap);
        System.out.println(instance);
    }


    @Test
    public void findPersonalTaskList() {
        //流程定义key
        String processDefinitionKey = "myEvection1";
        //任务负责人
        String assignee = "zhangsan";
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(processDefinitionKey)
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
