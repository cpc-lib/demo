package cc.ivera.test;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * 测试候选人
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:activiti-spring.xml")
public class TestCandidate5 {

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
     * act_ge_bytearray
     * act_re_deployment
     * act_re_procdef
     */
    @Test
    public void testDeployment() {
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程-Candidate")
                .addClasspathResource("bpmn/evection-candidate.bpmn")
                .deploy();
        System.out.println("流程部署id=" + deploy.getId());
        System.out.println("流程部署名字=" + deploy.getName());
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
        //通过部署id来删除流程部署信息
        //1,7501,70011,40001,67501,72519,72535,72551,35001,22501,72506
        String[] arr = {"1", "7501", "70011", "40001", "67501", "72519", "72535", "72551", "35001", "22501", "72506"};
        for (String s : arr) {
            String deploymentId = s;
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    public void getDeployment() throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("myEvection")
                .singleResult();
        String deploymentId = processDefinition.getDeploymentId();
        String pngName = processDefinition.getDiagramResourceName();
        String bpmnName = processDefinition.getResourceName();
        InputStream bpmnInput = repositoryService.getResourceAsStream(deploymentId, bpmnName);
        File bpmnFile = new File("d:/evectionflow01.bpmn");
        FileOutputStream bpmnOutStream = new FileOutputStream(bpmnFile);
        IOUtils.copy(bpmnInput, bpmnOutStream);
        bpmnOutStream.close();
        bpmnInput.close();
    }


    /**
     * 启动流程 的时候设置流程变量
     */
    @Test
    public void testStartProcess() {
        String key = "testCandidate";
        runtimeService.startProcessInstanceByKey(key);
    }


    /**
     * 完成个人任务
     */
    @Test
    public void completTask() {
        String key = "testCandidate";
        String assingee = "汤姆";
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee).list();
        for (Task task : list) {
            String id = task.getId();
            System.out.println(id);
            taskService.complete("102505");
            System.out.println("完成任务的提交");
        }
    }

    /**
     * 分发任务
     * 查询要进行审批的组任务列表
     */
    @Test
    public void findGroupTaskList() {
        String key = "testCandidate";
        String candidateUser = "lisi";
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskCandidateUser(candidateUser) //根据候选人查询任务
                .list();
        for (Task task : taskList) {
            System.out.println("========================");
            System.out.println("流程实例ID=" + task.getProcessInstanceId());
            System.out.println("任务id=" + task.getId());
            System.out.println("任务负责人=" + task.getAssignee());
        }
    }



    @Test
    public void findTaskList() {
        String key = "testCandidate";
        String assignee = "张财务";
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assignee) //根据负责人查询任务
                .list();
        for (Task task : taskList) {
            System.out.println("========================");
            System.out.println("流程实例ID=" + task.getProcessInstanceId());
            System.out.println("任务id=" + task.getId());
            System.out.println("任务负责人=" + task.getAssignee());
        }
    }

    /**
     * 获取活
     * 候选人拾取任务racing-operation
     */
    @Test
    public void claimTask() {
        String taskId = "105002";
        String candidateUser = "wangwu";
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(candidateUser).singleResult();
        if (task != null) {
            taskService.claim(taskId, candidateUser);
            System.out.println("taskid-" + taskId + "-用户-" + candidateUser + "-拾取任务完成");
        }
    }

    /**
     * 任务的归还 释放锁的操作
     */
    @Test
    public void testAssigneeToGroupTask() {
        String taskId = "105002";
        String assignee = "wangwu";
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(assignee).singleResult();
        if (task != null) {
            //taskService.setAssignee(taskId, null);
            taskService.unclaim(taskId);
            System.out.println("taskid-" + taskId + "-归还任务完成");
        }
    }


    /**
     * 交接
     * 任务的交接.切换任务的办理人，交接过程。
     */
    @Test
    public void testAssigneeToCandidateUser() {
        String taskId = "82502";
        String assignee = "wangwu";
        String candidateUser = "lisi";
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(assignee)
                .singleResult();
        if (task != null) {
            taskService.setAssignee(taskId, candidateUser);
            System.out.println("taskid-" + taskId + "-交接任务完成");
        }
    }

    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask2() {
        String key = "testCandidate";
        String assingee = "wangwu";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            taskService.complete(task.getId());
            System.out.println("完成任务的提交");
        }

    }

    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask3() {
        String key = "testCandidate";
        String assingee = "王总经理";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if (task != null) {
            taskService.complete(task.getId());
            System.out.println("完成任务的提交");
        }

    }


    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask4() {
        String key = "testCandidate";
        String assingee = "张财务";
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .list().get(0);
        if (task != null) {
            taskService.complete(task.getId());
            System.out.println("完成任务的提交"+task.getId());
        }

    }


}
