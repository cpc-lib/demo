package cc.ivera.test;

import org.activiti.engine.*;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.task.Task;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;


/**
 * 测试候选人
 */
public class TestCandidate5 {
    /**
     * 流程部署
     * act_ge_bytearray
     * act_re_deployment
     * act_re_procdef
     */
    @Test
    public void testDeployment() {
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取RepositoryServcie
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、使用service进行流程的部署，定义一个流程的名字，把bpmn和png部署到数据中
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程-Candidate")
                .addClasspathResource("bpmn/evection-candidate.bpmn")
                .deploy();
//        4、输出部署信息
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
    public void deleteDeployMent() {
//      获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        通过引擎来获取 RepositoryService
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        通过部署id来删除流程部署信息
        //1,7501,70011,40001,67501,72519,72535,72551,35001,22501,72506
        String[] arr = {"1", "7501", "70011", "40001", "67501", "72519", "72535", "72551", "35001", "22501", "72506"};
        for (String s : arr) {
            String deploymentId = s;
//        repositoryService.deleteDeployment(deploymentId);
            repositoryService.deleteDeployment(deploymentId, true);
        }
    }

    @Test
    public void getDeployment() throws IOException {
//         1、得到引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取api，RepositoryService
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、获取查询对象 ProcessDefinitionQuery查询流程定义信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("myEvection")
                .singleResult();
//        4、通过流程定义信息，获取部署ID
        String deploymentId = processDefinition.getDeploymentId();
//        5、通过RepositoryService ，传递部署id参数，读取资源信息（png 和 bpmn）
//          5.1、获取png图片的流
//        从流程定义表中，获取png图片的目录和名字
        String pngName = processDefinition.getDiagramResourceName();
//        通过 部署id和 文件名字来获取图片的资源
        InputStream pngInput = repositoryService.getResourceAsStream(deploymentId, pngName);
//          5.2、获取bpmn的流
        String bpmnName = processDefinition.getResourceName();
        InputStream bpmnInput = repositoryService.getResourceAsStream(deploymentId, bpmnName);
//        6、构造OutputStream流
        File pngFile = new File("d:/evectionflow01.png");
        File bpmnFile = new File("d:/evectionflow01.bpmn");
        FileOutputStream pngOutStream = new FileOutputStream(pngFile);
        FileOutputStream bpmnOutStream = new FileOutputStream(bpmnFile);
//        7、输入流，输出流的转换
        IOUtils.copy(pngInput, pngOutStream);
        IOUtils.copy(bpmnInput, bpmnOutStream);
//        8、关闭流
        pngOutStream.close();
        bpmnOutStream.close();
        pngInput.close();
        bpmnInput.close();
    }


    /**
     * 启动流程 的时候设置流程变量
     */
    @Test
    public void testStartProcess() {
//        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        流程定义的Key
        String key = "testCandidate";
//        启动流程
        runtimeService.startProcessInstanceByKey(key);
    }


    /**
     * 完成个人任务
     */
    @Test
    public void completTask() {
//        流程定义的Key
        String key = "testCandidate";
//        任务负责人
        String assingee = "汤姆";
//        String assingee = "";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee).list();
        for (Task task : list) {
            String id = task.getId();
            System.out.println(id);
            taskService.complete("102505");
            System.out.println("完成任务的提交");
        }

        //if (task != null) {
        //    //     根据任务id来   完成任务
        //    taskService.complete(task.getId());
        //    System.out.println("完成任务的提交");
        //}


    }

    /**
     * 分发任务
     * 查询要进行审批的组任务列表
     */
    @Test
    public void findGroupTaskList() {
        //        流程定义的Key
        String key = "testCandidate";
//        任务候选人
        String candidateUser = "lisi";
//        获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        查询组任务
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
        //        流程定义的Key
        String key = "testCandidate";
//        任务负责人
        String assignee = "张财务";
//        获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        查询组任务
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
//        获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        当前任务的id
        String taskId = "105002";
//        任务候选人
        String candidateUser = "wangwu";
//        查询任务
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskCandidateUser(candidateUser).singleResult();
        if (task != null) {
//            拾取任务竞争处理任务
            taskService.claim(taskId, candidateUser);
            System.out.println("taskid-" + taskId + "-用户-" + candidateUser + "-拾取任务完成");
        }
    }

    /**
     * 任务的归还 释放锁的操作
     */
    @Test
    public void testAssigneeToGroupTask() {
//        获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        当前任务的id
        String taskId = "105002";
//        任务负责人
        String assignee = "wangwu";
//        根据key 和负责人来查询任务
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(assignee).singleResult();
        if (task != null) {
//            归还任务 ,就是把负责人 设置为空
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
//        获取引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取TaskService
        TaskService taskService = processEngine.getTaskService();
//        当前任务的id(一个错误的taskid不会影响程序的运行)
        String taskId = "82502";
//        任务负责人
        String assignee = "wangwu";
//        任务候选人
        String candidateUser = "lisi";
//        根据key 和负责人来查询任务
        Task task = taskService.createTaskQuery()
                .taskId(taskId)
                .taskAssignee(assignee)
                .singleResult();
        if (task != null) {
//            交接任务 ,就是把负责人 设置为空
            taskService.setAssignee(taskId, candidateUser);
            System.out.println("taskid-" + taskId + "-交接任务完成");
        }
    }

    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask2() {
//        流程定义的Key
        String key = "testCandidate";
//        任务负责人
        String assingee = "wangwu";
//        String assingee = "";
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
            System.out.println("完成任务的提交");
        }

    }

    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask3() {
//        流程定义的Key
        String key = "testCandidate";
//        任务负责人
        String assingee = "王总经理";
//        String assingee = "";
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
            System.out.println("完成任务的提交");
        }

    }


    /**
     * 完成部门经理审批任务
     */
    @Test
    public void completTask4() {
//        流程定义的Key
        String key = "testCandidate";
//        任务负责人
        String assingee = "张财务";
//        String assingee = "";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .list().get(0);
        if (task != null) {
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
            System.out.println("完成任务的提交"+task.getId());
        }

    }


}
