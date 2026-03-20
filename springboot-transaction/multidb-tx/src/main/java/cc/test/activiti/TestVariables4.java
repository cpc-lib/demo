package cc.ivera.test.activiti;

import cc.ivera.Application;
import cc.ivera.domain.Evection;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * 测试流程变量
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes={Application.class})
public class TestVariables4 {

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
                .name("出差申请流程-variables")
                .addClasspathResource("processes/evection-global.bpmn").deploy();
        System.out.println("流程部署id=" + deploy.getId());
        System.out.println("流程部署名字=" + deploy.getName());
    }

    /**
     * 启动流程 的时候设置流程变量
     * 设置流程变量num
     * 设置任务负责人
     */
    @Test
    public void testStartProcess() {
        String key = "myEvection2";
        Map<String, Object> variables = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(2d);
        variables.put("evection", evection);
        variables.put("assignee0", "李四");
        variables.put("assignee1", "王经理");
        variables.put("assignee2", "杨总经理");
        variables.put("assignee3", "张财务");
        runtimeService.startProcessInstanceByKey(key, variables);
    }

    /**
     * 完成个人任务 李四 王经理 杨总经理 张财务
     */
    @Test
    public void completeTask() {
        String key = "myEvection2";
        String assingee = "张财务";
        Task task = taskService.createTaskQuery().processDefinitionKey(key).taskAssignee(assingee).singleResult();
        if (task != null) {
            taskService.complete(task.getId());
        }

    }

    /**
     * ##### 任务办理时设置变量
     * <p>
     * 在完成任务时设置流程变量，该流程变量只有在该任务完成后其它结点才可使用该变量，
     * 它的作用域是整个流程实例，如果设置的流程变量的key在流程实例中已存在相同的名字则后设置的变量替换前边设置的变量。
     * 完成任务，判断当前用户是否有权限
     */
    @Test
    public void completeTask2() {
        String key = "myEvection2";
        String assingee = "李四";
        Map<String, Object> map = new HashMap<>();
        Evection evection = new Evection();
        evection.setNum(5d);
        map.put("evection", evection);
        Task task = taskService.createTaskQuery().
                processDefinitionKey(key).
                taskAssignee(assingee).
                singleResult();
        if (task != null) {
            //完成任务设置流程变量值
            taskService.complete(task.getId(), map);
            System.out.println("任务执行完成");
        }
    }

    //通过流程实例id设置全局变量，该流程实例必须未执行完成。
    //注意：
    //executionId必须当前未结束 流程实例的执行id，通常此id设置流程实例 的id。也可以通runtimeService.getVariable()获取流程变量。
    //act_ru_execution
    @Test
    public void setGlobalVariableByExecutionId() {
        String executionId = "45002";
        Evection evection = new Evection();
        evection.setNum(3d);
        runtimeService.setVariable(executionId, "evection", evection);
    }

    //通过当前任务设置
    //任务id必须是当前待办任务id，act_ru_task中存在。如果该任务已结束，会报错
    //也可以通过taskService.getVariable()获取流程变量。
    //act_ru_task
    @Test
    public void setGlobalVariableByTaskId() {
        //当前待办任务id
        String taskId = "45005";
        Evection evection = new Evection();
        evection.setNum(3d);
        //通过任务设置流程变量
        taskService.setVariable(taskId, "evection", evection);
        //一次设置多个值
        //taskService.setVariables(taskId, variables)
    }


    //根据taskId获取到全局变量
    @Test
    public void getGlobalVariableByTaskId() {
        String taskId = "45005";
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        Map<String, Object> variables = taskService.getVariables(taskId);
        Evection evection = (Evection) (variables.get("evection"));
        assertEquals(evection.getNum(), 3d);
    }

    /* 设置流程变量会在当前执行流程变量表插入记录，同时也会在历史流程变量表也插入记录。
    记录当前运行流程实例可使用的流程变量，包括 global和local变量
    --当前流程变量表
    SELECT * FROM act_ru_variable
    Id_：主键
    Type_：变量类型
    Name_：变量名称
    Execution_id_：所属流程实例执行id，global和local变量都存储
    Proc_inst_id_：所属流程实例id，global和local变量都存储
    Task_id_：所属任务id，local变量存储
    Bytearray_：serializable类型变量存储对应act_ge_bytearray表的id
    Double_：double类型变量值
    Long_：long类型变量值
    Text_：text类型变量值

    --历史流程变量表
    SELECT * FROM act_hi_varinst
    记录所有已创建的流程变量，包括 global和local变量
    字段意义参考当前流程变量表。 */

    //设置local流程变量

    //任务办理时设置local流程变量，当前运行的流程实例只能在该任务结束前使用，任务结束该变量无法在当前流程实例使用，可以通过查询历史任务查询。

    /*
     *处理任务时设置local流程变量
     设置作用域为任务的local变量，每个任务可以设置同名的变量，互不影响。
     */
    @Test

    public void completeTaskByTaskId() {
        String taskId = "45005";
        Map<String, Object> variables = new HashMap<String, Object>();
        Evection evection = new Evection();
        evection.setNum(3d);
        Map<String, Object> var1 = new HashMap<String, Object>();
        variables.put("evection", evection);
        taskService.setVariablesLocal(taskId, var1);
        taskService.complete(taskId);
    }

    //任务id必须是当前待办任务id，act_ru_task中存在
    @Test
    public void setLocalVariableByTaskId() {
        String taskId = "55002";
        Evection evection = new Evection();
        evection.setNum(3d);
        taskService.setVariableLocal(taskId, "evection", evection);
    }

    //注意：查询历史流程变量，特别是查询pojo变量需要经过反序列化，不推荐使用。
    //act_hi_taskinst
    @Test
    public void findHistoricTask() {
        //创建历史任务查询对象
        HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService
                .createHistoricTaskInstanceQuery().unfinished();
        //查询结果包括local变量
        List<HistoricTaskInstance> list = historicTaskInstanceQuery.includeTaskLocalVariables().list();
        for (HistoricTaskInstance historicTaskInstance : list) {
            System.out.println("==============================");
            System.out.println("任务id：" + historicTaskInstance.getId());
            System.out.println("任务名称：" + historicTaskInstance.getName());
            System.out.println("任务负责人：" + historicTaskInstance.getAssignee());
            System.out.println("任务local变量：" + historicTaskInstance.getTaskLocalVariables());
        }
    }


}
