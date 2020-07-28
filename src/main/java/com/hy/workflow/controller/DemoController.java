package com.hy.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hy.workflow.service.MyProcessService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@Api(value = "例子", tags = "Demo", description = "测试DEMO")
public class DemoController {


    @Autowired
    protected RepositoryService repositoryService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected TaskService taskService;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private MyProcessService myProcessService;

    @PostMapping(value="/sayHello",produces = "application/json")
    @ApiOperation(value = "创建模型", notes = "创建一个流程模型")
    public String sayHello(
            @ApiParam(required = true, name = "userName", value = "用户名") @RequestParam String userName
    ) {
        return "你好: "+userName;
    }

    @PostMapping(value="/process")
    public void startProcessInstance(String processKey) {
        myProcessService.startProcess(processKey);
    }


    @GetMapping(value="/startProcess")
    public void startProcess(String processKey) {
        myProcessService.startProcess(processKey);
    }

    @RequestMapping(value="/tasks", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public List<TaskRepresentation> getTasks(@RequestParam String assignee) {
        List<Task> tasks = myProcessService.getTasks(assignee);
        List<TaskRepresentation> dtos = new ArrayList<TaskRepresentation>();
        for (Task task : tasks) {
            dtos.add(new TaskRepresentation(task.getId(), task.getName()));
        }
        return dtos;
    }

    static class TaskRepresentation {

        private String id;
        private String name;

        public TaskRepresentation(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }
        public void setId(String id) {
            this.id = id;
        }
        public String getName() {
            return name;
        }
        public void setName(String name) {
            this.name = name;
        }

    }


    @RequestMapping(value = "/hello",method = RequestMethod.GET)
    public String hello() {
        return "Hello World , I am a Demo!";
    }

    public static void main(String[] args) {
        ProcessEngineConfiguration cfg = new StandaloneProcessEngineConfiguration()
                .setJdbcUrl("jdbc:mysql://localhost:3306/flowable?useSSL=false&useUnicode=true&characterEncoding=UTF-8&allowMultiQueries=true&serverTimezone=Asia/Shanghai&nullCatalogMeansCurrent=true")
                .setJdbcUsername("zhaoyao")
                .setJdbcPassword("123456")
                .setJdbcDriver("com.mysql.cj.jdbc.Driver")
                .setDatabaseSchemaUpdate(ProcessEngineConfiguration.DB_SCHEMA_UPDATE_TRUE);

        ProcessEngine processEngine = cfg.buildProcessEngine();

        //部署流程定义
        Deployment deployment = deploymentModel(processEngine);

        //查询定义
        RepositoryService repositoryService = processEngine.getRepositoryService();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
                .deploymentId(deployment.getId())
                .singleResult();
        System.out.println("Found process definition : " + processDefinition.getName());

        //启动流程实例
        RuntimeService runtimeService = processEngine.getRuntimeService();
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", "张三");
        variables.put("nrOfHolidays", "3");
        variables.put("description", "我有事你呢哥哥");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("HTFWSP", variables);


        TaskService taskService = processEngine.getTaskService();
        List<Task> tasks = taskService.createTaskQuery().list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        //使用任务标识符，我们现在可以获得特定的流程实例变量，并在屏幕上显示实际请求
        Task task = tasks.get(0);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +processVariables.get("nrOfHolidays") + " Do you approve this?");


        HashMap vs = new HashMap<String, Object>();
        vs.put("approved", "通过3333333！");
        taskService.complete(task.getId(), vs);


    }

    //部署流程定义
    public static  Deployment deploymentModel(ProcessEngine processEngine){
        RepositoryService repositoryService = processEngine.getRepositoryService();
        Deployment deployment = repositoryService.createDeployment()
                .addClasspathResource("processes/HTFWSP.bpmn")
                .deploy();
        return  deployment;
    }










    @GetMapping(value = "/deploy/startProcessKey/{key}")
    public String startProcessKey(@PathVariable("key") String key) {
        Map<String, Object> variables = new HashMap<String, Object>();
        variables.put("employee", "雄安命");
        variables.put("nrOfHolidays", "1");
        variables.put("description", "在干嘛呢");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key,variables);

        return "好啊哈哈哈";
    }

    @GetMapping(value = "/process/completeTask")
    public String completeTask() {

        List<Task> tasks = taskService.createTaskQuery().list();
        System.out.println("You have " + tasks.size() + " tasks:");
        for (int i=0; i<tasks.size(); i++) {
            System.out.println((i+1) + ") " + tasks.get(i).getName());
        }

        Task task = tasks.get(0);
        Map<String, Object> processVariables = taskService.getVariables(task.getId());
        System.out.println(processVariables.get("employee") + " wants " +processVariables.get("nrOfHolidays") + " Do you approve this?");

        HashMap variables = new HashMap<String, Object>();
        variables.put("approved", "通过3333333！");
        taskService.complete(task.getId(), variables);

        return "好啊哈哈哈";
    }








}
