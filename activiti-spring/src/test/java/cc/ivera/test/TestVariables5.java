package cc.ivera.test;

import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试流程变量
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:activiti-spring.xml")
public class TestVariables5 {


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
     * 流程部署
     */
    @Test
    public void testDeployment(){
        //使用service进行流程的部署，定义一个流程的名字，把bpmn和png部署到数据中
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程-variables1")
                .addClasspathResource("bpmn/evection-global.bpmn")
                .deploy();
        //输出部署信息
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
        //流程定义的Key
        String key = "myEvection2";
        //流程变量的map
        Map<String,Object> variables = new HashMap<>();
        //设置流程变量
        Evection evection = new Evection();
        //设置出差日期
        evection.setNum(3d);
        //把流程变量的pojo放入map
        variables.put("evection",evection);
        //设定任务的负责人
        variables.put("assignee0","李四1");
        variables.put("assignee1","王经理1");
        variables.put("assignee2","杨总经理1");
        variables.put("assignee3","张财务1");
        //启动流程
        runtimeService.startProcessInstanceByKey(key,variables);
    }

    /**
     * 完成个人任务
     */
    @Test
    public void completTask(){
        //流程定义的Key
        String key = "myEvection2";
        //任务负责人
        String assingee = "张财务1";
        //查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            //根据任务id来完成任务
            taskService.complete(task.getId());
            System.out.println(task.getId()+"----任务已完成");
        }

    }
}
