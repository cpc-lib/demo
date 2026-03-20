package cc.ivera.test;

import cc.ivera.demo.pojo.Evection;
import org.activiti.engine.*;
import org.activiti.engine.history.HistoricTaskInstance;
import org.activiti.engine.history.HistoricTaskInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.task.Task;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static junit.framework.Assert.assertEquals;

/**
 * 测试流程变量
 */
public class TestVariables4 {
    /**
     * 流程部署
     */
    @Test
    public void testDeployment(){
//        1、创建ProcessEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        2、获取RepositoryServcie
        RepositoryService repositoryService = processEngine.getRepositoryService();
//        3、使用service进行流程的部署，定义一个流程的名字，把bpmn和png部署到数据中
        Deployment deploy = repositoryService.createDeployment()
                .name("出差申请流程-variables")
                .addClasspathResource("bpmn/evection-global.bpmn")
                .deploy();
//        4、输出部署信息
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
//        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RunTimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        流程定义的Key
        String key = "myEvection2";
//        流程变量的map
        Map<String,Object> variables = new HashMap<>();
//        设置流程变量
        Evection evection = new Evection();
//        设置出差日期
        evection.setNum(2d);
//        把流程变量的pojo放入map
        variables.put("evection",evection);
//        设定任务的负责人
        variables.put("assignee0","李四");
        variables.put("assignee1","王经理");
        variables.put("assignee2","杨总经理");
        variables.put("assignee3","张财务");
//        启动流程
        runtimeService.startProcessInstanceByKey(key,variables);
    }

    /**
     * 完成个人任务
     */
    @Test
    public void completTask(){
//        流程定义的Key
        String key = "myEvection2";
//        任务负责人
//        String assingee = "李四";

        String assingee = "张财务";
        //        获取流程引擎
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取taskservice
        TaskService taskService = processEngine.getTaskService();
//        查询任务
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            //     根据任务id来   完成任务
            taskService.complete(task.getId());
        }

    }

    /**
     * ##### 任务办理时设置变量
     *
     * 在完成任务时设置流程变量，该流程变量只有在该任务完成后其它结点才可使用该变量，
     * 它的作用域是整个流程实例，如果设置的流程变量的key在流程实例中已存在相同的名字则后设置的变量替换前边设置的变量。
     * 完成任务，判断当前用户是否有权限
     */
    @Test
    public void completTask2() {
        //任务id
        String key = "myEvection2";
//        任务负责人
        String assingee = "张三";
//       获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//       创建TaskService
        TaskService taskService = processEngine.getTaskService();
//       创建变量集合
        Map<String, Object> map = new HashMap<>();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置出差天数
        evection.setNum(2d);
//      定义流程变量
        map.put("evection",evection);
//        完成任务前，需要校验该负责人可以完成当前任务
//        校验方法：
//        根据任务id和任务负责人查询当前任务，如果查到该用户有权限，就完成
        Task task = taskService.createTaskQuery()
                .processDefinitionKey(key)
                .taskAssignee(assingee)
                .singleResult();
        if(task != null){
            //完成任务是，设置流程变量的值
            taskService.complete(task.getId(),map);
            System.out.println("任务执行完成");
        }
    }

    //通过流程实例id设置全局变量，该流程实例必须未执行完成。
    //注意：
    //executionId必须当前未结束 流程实例的执行id，通常此id设置流程实例 的id。也可以通runtimeService.getVariable()获取流程变量。
    @Test
    public void setGlobalVariableByExecutionId(){
//    当前流程实例执行 id，通常设置为当前执行的流程实例
        String executionId="5002";
//     获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
//        获取RuntimeService
        RuntimeService runtimeService = processEngine.getRuntimeService();
//        创建出差pojo对象
        Evection evection = new Evection();
//        设置天数
        evection.setNum(3d);
//      通过流程实例 id设置流程变量
        runtimeService.setVariable(executionId, "evection", evection);
//      一次设置多个值
//      runtimeService.setVariables(executionId, variables)
    }

    //通过当前任务设置
    //任务id必须是当前待办任务id，act_ru_task中存在。如果该任务已结束，会报错
    //也可以通过taskService.getVariable()获取流程变量。
    @Test
    public void setGlobalVariableByTaskId(){
        //当前待办任务id
        String taskId="5005";
//     获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        Evection evection = new Evection();
        evection.setNum(3d);
        //通过任务设置流程变量
        taskService.setVariable(taskId, "evection", evection);
        //一次设置多个值
        //taskService.setVariables(taskId, variables)
    }


    //根据taskId获取到全局变量
    @Test
    public void getGlobalVariableByTaskId(){
        //当前待办任务id
        String taskId="5005";
//     获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        //通过任务设置流程变量
        Map<String, Object> variables = taskService.getVariables(taskId);
        Evection evection = (Evection)(variables.get("evection"));
        //断言工具类
        assertEquals(evection.getNum(),3d);
        //System.out.println(num);
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

    public void completTask3() {
        //任务id
        String taskId = "57502";
//  获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
//  定义流程变量
        Map<String, Object> variables = new HashMap<String, Object>();
        Evection evection = new Evection ();
        evection.setNum(3d);
// 定义流程变量
        Map<String, Object> var1 = new HashMap<String, Object>();
//  变量名是holiday，变量值是holiday对象
        variables.put("evection", evection);
//  设置local变量，作用域为该任务
        taskService.setVariablesLocal(taskId, var1);
//  完成任务
        taskService.complete(taskId);
    }

    //任务id必须是当前待办任务id，act_ru_task中存在
    @Test
    public void setLocalVariableByTaskId(){
//   当前待办任务id
        String taskId="60002";
//  获取processEngine
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        TaskService taskService = processEngine.getTaskService();
        Evection evection = new Evection ();
        evection.setNum(3d);
//  通过任务设置流程变量
        taskService.setVariableLocal(taskId, "evection", evection);
//  一次设置多个值
        //taskService.setVariablesLocal(taskId, variables)
    }

    //注意：查询历史流程变量，特别是查询pojo变量需要经过反序列化，不推荐使用。
    //act_hi_taskinst
    @Test
    public void test(){
        ProcessEngine processEngine = ProcessEngines.getDefaultProcessEngine();
        HistoryService historyService = processEngine.getHistoryService();
        // 创建历史任务查询对象
        HistoricTaskInstanceQuery historicTaskInstanceQuery = historyService.createHistoricTaskInstanceQuery();
        // 查询结果包括 local变量
        List<HistoricTaskInstance> list = historicTaskInstanceQuery.includeTaskLocalVariables().list();
        System.out.println(list.size());
        for (HistoricTaskInstance historicTaskInstance : list) {
            System.out.println("==============================");
            System.out.println("任务id：" + historicTaskInstance.getId());
            System.out.println("任务名称：" + historicTaskInstance.getName());
            System.out.println("任务负责人：" + historicTaskInstance.getAssignee());
            System.out.println("任务local变量："+ historicTaskInstance.getTaskLocalVariables());
        }
    }




}
