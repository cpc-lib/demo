package cc.ivera.test.activiti;

import cc.ivera.Application;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.repository.ProcessDefinitionQuery;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipInputStream;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={Application.class})
public class ActivitiDemo0 {


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
    public void testDeployment() {
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程")
                .addClasspathResource("processes/evection.bpmn")
                .deploy();
        System.out.println("流程部署id=" + deploy.getId());
        System.out.println("流程部署名字=" + deploy.getName());
    }


    /**
     * 使用zip包进行批量的部署
     */
    @Test
    public void deployProcessByZip() {
        //读取资源包文件，构造成inputStream
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("processes/evection.zip");
        //用inputStream 构造ZipInputStream
        ZipInputStream zipInputStream = new ZipInputStream(inputStream);
        //使用压缩包的流进行流程的部署
        Deployment deploy = repositoryService.createDeployment()
                .addZipInputStream(zipInputStream)
                .deploy();
        System.out.println("流程部署id=" + deploy.getId());
        System.out.println("流程部署的名称=" + deploy.getName());
    }

    /**
     * 启动流程实例
     * `act_hi_actinst`    流程实例执行历史信息
     * `act_hi_identitylink` 流程参与用户的历史信息
     * `act_hi_procinst`     流程实例的历史信息
     * `act_hi_taskinst`     流程任务的历史信息
     * `act_ru_execution`    流程执行信息
     * `act_ru_identitylink`  流程的正在参与用户信息
     * `act_ru_task`         流程当前任务信息
     */
    @Test
    public void testStartProcess() {
        ProcessInstance instance = runtimeService.startProcessInstanceByKey("myEvection");
        System.out.println("流程定义ID：" + instance.getProcessDefinitionId());
        System.out.println("流程实例ID：" + instance.getId());
        System.out.println("当前活动的ID：" + instance.getActivityId());
    }


    /**
     * 查询个人待执行的任务
     */
    @Test
    public void testFindPersonalTaskList() {
        List<Task> taskList = taskService.createTaskQuery()
                .processDefinitionKey("myEvection") //流程Key
                .taskAssignee("zhangsan")  //要查询的负责人
                .list();
        for (Task task : taskList) {
            System.out.println("流程实例id=" + task.getProcessInstanceId());
            System.out.println("任务Id=" + task.getId());
            System.out.println("任务负责人=" + task.getAssignee());
            System.out.println("任务名称=" + task.getName());
            taskService.complete(task.getId());
        }
    }

    /**
     * 完成个人任务jerry jack rose
     */
    @Test
    public void completTask() {
        Task task = taskService.createTaskQuery()
                .processDefinitionKey("myEvection")
                .taskAssignee("rose")
                .singleResult();

        System.out.println("流程实例id=" + task.getProcessInstanceId());
        System.out.println("任务Id=" + task.getId());
        System.out.println("任务负责人=" + task.getAssignee());
        System.out.println("任务名称=" + task.getName());
        taskService.complete(task.getId());
    }

    /**
     * 查询流程定义
     */
    @Test
    public void queryProcessDefinition() {
        ProcessDefinitionQuery definitionQuery = repositoryService.createProcessDefinitionQuery();
        List<ProcessDefinition> definitionList = definitionQuery.processDefinitionKey("myEvection")
                .orderByProcessDefinitionVersion()
                .desc()
                .list();
        for (ProcessDefinition processDefinition : definitionList) {
            System.out.println("流程定义ID：" + processDefinition.getId());
            System.out.println("流程定义名称:" + processDefinition.getName());
            System.out.println("流程定义Key:" + processDefinition.getKey());
            System.out.println("流程定义版本:" + processDefinition.getVersion());
            System.out.println("流程部署ID:" + processDefinition.getDeploymentId());
        }
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
        String deploymentId = "15001";
        repositoryService.deleteDeployment(deploymentId, true);
    }


    /**
     * 下载 资源文件
     * 方案1： 使用Activiti提供的api，来下载资源文件,保存到文件目录
     * 方案2： 自己写代码从数据库中下载文件，使用jdbc对blob 类型，blob类型数据读取出来，保存到文件目录
     * 解决Io操作：commons-io.jar
     * 这里我们使用方案1，Activiti提供的api：RespositoryService
     */
    @Test
    public void getDeployment() throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey("myEvection")
                .singleResult();
        //通过流程定义信息，获取部署ID
        String deploymentId = processDefinition.getDeploymentId();
        //通过RepositoryService ，传递部署id参数，读取资源信息（png 和 bpmn）
        //获取png图片的流
        //从流程定义表中获取png图片的目录和名字
        String pngName = processDefinition.getDiagramResourceName();
        //通过部署id和文件名字来获取图片的资源
        //InputStream pngInput = repositoryService.getResourceAsStream(deploymentId, pngName);
        //获取bpmn的流
        String bpmnName = processDefinition.getResourceName();
        InputStream bpmnInput = repositoryService.getResourceAsStream(deploymentId, bpmnName);
        //必须先存在这个目录才可以通过
        File bpmnFile = new File("d:\\hom\\process\\evection.bpmn");
        FileOutputStream bpmnOutStream = new FileOutputStream(bpmnFile);
        IOUtils.copy(bpmnInput, bpmnOutStream);
        bpmnOutStream.close();
        bpmnInput.close();
    }

    /**
     * 查看历史信息
     */
    @Test
    public void findHistoryInfo() {
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        instanceQuery.processDefinitionId("myEvection:1:3");
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();
        for (HistoricActivityInstance hi : activityInstanceList) {
            System.out.println(hi.getActivityId());
            System.out.println(hi.getActivityName());
            System.out.println(hi.getProcessDefinitionId());
            System.out.println(hi.getProcessInstanceId());
            System.out.println("<==========================>");
        }
    }
}
