package com.hy.workflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.service.DemoService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.*;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.rest.service.api.repository.ModelResponse;
import org.flowable.task.api.Task;
import org.flowable.ui.common.util.XmlUtil;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.springframework.web.multipart.MultipartFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

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
    protected ModelRepository modelRepository;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    private DemoService demoService;

    @PostMapping(value="/sayHello",produces = "application/json")
    @ApiOperation(value = "创建模型", notes = "创建一个流程模型")
    public String sayHello(@ApiParam(required = true, name = "userName") @RequestParam String userName) {
        return "你好: "+userName;
    }


    @ApiOperation(value = "导入模型", tags = { "Models" })
    @PostMapping(value = "/models/importModel")
    public List<ModelResponse> importModel(@RequestParam("modelfile") MultipartFile uploadFile) {
        String fileName = uploadFile.getOriginalFilename();
        List<ModelResponse> modelList = new ArrayList<>();
        try {

            XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
            InputStreamReader xmlIn = new InputStreamReader(uploadFile.getInputStream(), StandardCharsets.UTF_8);
            XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
            BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
            BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
            if (bpmnModel.getLocationMap().size() == 0) {
                BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                bpmnLayout.execute();
            }

            Process process = bpmnModel.getMainProcess();
            String key = process.getId();
            String name = StringUtils.isNotEmpty(process.getName())?process.getName():process.getId();
            String description = process.getDocumentation();

            BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
            ObjectNode bpmJson = bpmnJsonConverter.convertToJson(bpmnModel);

            Model model = new Model();
            List<Model> models = modelRepository.findByKeyAndType(key, AbstractModel.MODEL_TYPE_BPMN);
            if(CollectionUtils.isNotEmpty(models)){
                throw new WorkflowException("模型标识为："+key+" 的模型已经存在！");
            }

            model.setName(name);
            model.setKey(process.getId());
            model.setModelType(AbstractModel.MODEL_TYPE_BPMN);
            model.setCreated(Calendar.getInstance().getTime());
            model.setCreatedBy("zhangsan");
            model.setDescription(description);
            model.setModelEditorJson(bpmJson.toString());
            model.setLastUpdated(Calendar.getInstance().getTime());
            model.setLastUpdatedBy("zhangsan");
            modelService.saveModel(model);

            /*if(!process.getExtensionElements().containsKey("businessId")){
                ExtensionElement businessElement = new ExtensionElement();
                businessElement.setName("businessId");
                businessElement.setElementText("CONTRACT");
                process.addExtensionElement(businessElement);
            }*/

/*
            Model model =  repositoryService.newModel() ;
            model.setName(name);
            model.setKey(key);
            repositoryService.saveModel(model);

            BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
            ObjectNode bpmJson = bpmnJsonConverter.convertToJson(bpmnModel);
            ObjectNode propertiesNode = (ObjectNode)bpmJson.get("properties");

            bpmJson.put("modelId",model.getId());
            propertiesNode.put("process_id",key);
            propertiesNode.put("process_author","zhaosan"); //TODO 创建人应该是当前登录用户

            //设置MetaInfo信息
            ObjectNode metaInfoNode = new ObjectMapper().createObjectNode();
            metaInfoNode.put("id", model.getId());
            metaInfoNode.put("process_id", key);
            ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            metaInfoNode.set("stencilset", stencilSetNode);
            //MetaInfo的properties
            ObjectNode propNode = this.objectMapper.createObjectNode();
            propNode.put("name", name);
            if(StringUtils.isNotBlank(description)) propNode.put("description", description);
            propNode.put("model_type", "model");
            //TODO 创建人、单位、部门 应该是当前登录用户所在的信息
            propNode.put("process_author", "zhaosan");
            propNode.put("department_id", "2000012");
            propNode.put("unit_id", "2000000");
            metaInfoNode.set("properties", propNode);
            model.setMetaInfo(metaInfoNode.toString());

            repositoryService.saveModel(model);
            repositoryService.addModelEditorSource(model.getId(),bpmJson.toString().getBytes());
*/

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return modelList;
    }



    @GetMapping(value = "/rejectTask/{taskId}")
    public String rejectTask(@RequestParam String taskId,@RequestParam String targetNodeId) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();


        // moveActivityIdsToSingleActivityId  这可以用于并行执行，如并行/包容网关
        List<String> sourceNodes = new ArrayList<>();
        sourceNodes.add(task.getTaskDefinitionKey());
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdsToSingleActivityId(sourceNodes, targetNodeId)
                .changeState();


        /*List<String > executionIds = new ArrayList<>();
        Execution currentExecution= runtimeService.createExecutionQuery().executionId(task.getProcessInstanceId()).singleResult();
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(currentExecution.getRootProcessInstanceId()).list();
        for (Execution execution : executions) {
            //if(execution.getActivityId()!=null&&execution.getActivityId().equals("CCC")){
                executionIds.add(execution.getId());
            //}
        }
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(executionIds, targetNodeId)
                .changeState();*/

       /* List<String> sourceNodes = new ArrayList<>();
        sourceNodes.add(task.getTaskDefinitionKey());
        runtimeService.createChangeActivityStateBuilder()
                .processInstanceId(task.getProcessInstanceId())
                .moveActivityIdToParentActivityId(task.getTaskDefinitionKey(), targetNodeId)
                .changeState();*/

        /*List<String> sourceNodes = new ArrayList<>();
        //sourceNodes.add("ZiLiuCheng");
        sourceNodes.add(task.getTaskDefinitionKey());
        ChangeActivityStateBuilderImpl builder = (ChangeActivityStateBuilderImpl)runtimeService.createChangeActivityStateBuilder();
        builder.moveActivityIdsToParentActivityId(sourceNodes, targetNodeId,null).processInstanceId(task.getProcessInstanceId()).changeState();*/

        /*List<String > currentExecutionIds = new ArrayList<>();
        List<Execution> executions = runtimeService.createExecutionQuery().parentId(task.getProcessInstanceId()).list();
        for (Execution execution : executions) {
            System.out.println("并行网关节点数："+execution.getActivityId());
            currentExecutionIds.add(execution.getId());
        }
        runtimeService.createChangeActivityStateBuilder()
                .moveExecutionsToSingleActivityId(currentExecutionIds, targetNodeId)
                .changeState();*/



        return "SUCCESS";
    }




    @GetMapping(value="/startProcess")
    public void startProcess(String processKey) {
        demoService.startProcess(processKey);
    }

    @RequestMapping(value="/tasks", method= RequestMethod.GET, produces= MediaType.APPLICATION_JSON_VALUE)
    public List<TaskRepresentation> getTasks(@RequestParam String assignee) {
        List<Task> tasks = demoService.getTasks(assignee);
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
        variables.put("employee", "白居易");
        variables.put("nrOfHolidays", "3");
        variables.put("description", "秋天殊未晓，风雨正苍苍");
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
        vs.put("approved", "通过，OK的！");
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
        variables.put("employee", "李白");
        variables.put("nrOfHolidays", "1");
        variables.put("description", "将进酒");
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(key,variables);
        return "直挂云帆济沧海";
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
        variables.put("approved", "通过！");
        taskService.complete(task.getId(), variables);

        return "审批完成";
    }








}
