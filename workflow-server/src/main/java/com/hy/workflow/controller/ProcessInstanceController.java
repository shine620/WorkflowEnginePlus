package com.hy.workflow.controller;

import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ProcessInstanceModel;
import com.hy.workflow.model.StartProcessRequest;
import com.hy.workflow.service.ProcessInstanceService;
import com.hy.workflow.util.EntityModelUtil;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.ProcessInstanceQueryProperty;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstanceQuery;
import org.flowable.engine.task.Comment;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceQueryRequest;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

@RestController
@RequestMapping("/ProcessInstanceController")
@CrossOrigin
@Api(value = "流程实例", tags = "Process Instances", description = "流程实例接口")
public class ProcessInstanceController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    protected RuntimeService runtimeService;

    @Autowired
    protected HistoryService historyService;

    @Autowired
    private ProcessInstanceService processInstanceService;

    @Autowired
    private TaskService taskService;


    @ApiOperation(value = "查询流程实例列表", notes = "流程原始数据", tags = {"Process Instances"})
    @PostMapping(value = "/process-instances", produces = "application/json")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "nameLikeIgnoreCase", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionKey", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionCategory", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionVersion", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "processDefinitionEngineVersion", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "businessKey", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "businessKeyLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startedBy", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startedBefore", dataType = "string", format = "date-time", paramType = "query"),
            @ApiImplicitParam(name = "startedAfter", dataType = "string", format = "date-time", paramType = "query"),
            @ApiImplicitParam(name = "involvedUser", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "suspended", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "superProcessInstanceId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "subProcessInstanceId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "excludeSubprocesses", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "includeProcessVariables", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "callbackId", dataType = "string",paramType = "query"),
            @ApiImplicitParam(name = "callbackType", dataType = "string",paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", allowableValues = "id,processDefinitionId,tenantId,processDefinitionKey", paramType = "query"),
    })
    public DataResponse<ProcessInstanceResponse> queryProcessInstances(
            @ApiParam @RequestParam(defaultValue = "1") Integer startPage,@ApiParam @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestBody ProcessInstanceQueryRequest queryRequest,
            @ApiParam(hidden = true) @RequestParam Map<String, String> allRequestParams) {

        ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
        if (allRequestParams.containsKey("id") && StringUtils.isNotBlank(allRequestParams.get("id")) ) {
            query.processDefinitionId(allRequestParams.get("id"));
        }
        if (allRequestParams.containsKey("name") && StringUtils.isNotBlank(allRequestParams.get("name")) ) {
            query.processInstanceName(allRequestParams.get("name"));
        }
        if (allRequestParams.containsKey("nameLike") && StringUtils.isNotBlank(allRequestParams.get("name")) ) {
            query.processInstanceNameLike(allRequestParams.get("nameLike"));
        }
        if (allRequestParams.containsKey("processDefinitionId") && StringUtils.isNotBlank(allRequestParams.get("processDefinitionId")) ) {
            query.processDefinitionId(allRequestParams.get("processDefinitionId"));
        }
        if (allRequestParams.containsKey("businessKey") && StringUtils.isNotBlank(allRequestParams.get("businessKey")) ) {
            query.processInstanceBusinessKey(allRequestParams.get("businessKey"));
        }

        PaginateRequest paginateRequest = new PaginateRequest();
        paginateRequest.setStart( (startPage-1)*pageSize );
        paginateRequest.setSize(Integer.valueOf(allRequestParams.get("pageSize")));

        Map<String, QueryProperty> allowedSortProperties = new HashMap<>();
        allowedSortProperties.put("processDefinitionId", ProcessInstanceQueryProperty.PROCESS_DEFINITION_ID);
        allowedSortProperties.put("processDefinitionKey", ProcessInstanceQueryProperty.PROCESS_DEFINITION_KEY);
        allowedSortProperties.put("id", ProcessInstanceQueryProperty.PROCESS_INSTANCE_ID);
        allowedSortProperties.put("startTime", ProcessInstanceQueryProperty.PROCESS_START_TIME);
        allowedSortProperties.put("tenantId", ProcessInstanceQueryProperty.TENANT_ID);

        DataResponse<ProcessInstanceResponse> responseList = paginateList(allRequestParams, paginateRequest, query, "id", allowedSortProperties, EntityModelUtil::toProcessInstanceResponseList);

        //设置流程定义信息
        Set<String> processDefinitionIds = new HashSet<>();
        List<ProcessInstanceResponse> processInstanceList = responseList.getData();
        for (ProcessInstanceResponse processInstanceResponse : processInstanceList) {
            if (!processDefinitionIds.contains(processInstanceResponse.getProcessDefinitionId())) {
                processDefinitionIds.add(processInstanceResponse.getProcessDefinitionId());
            }
        }
        if (processDefinitionIds.size() > 0) {
            List<ProcessDefinition> processDefinitionList = repositoryService.createProcessDefinitionQuery().processDefinitionIds(processDefinitionIds).list();
            Map<String, ProcessDefinition> processDefinitionMap = new HashMap<>();
            for (ProcessDefinition processDefinition : processDefinitionList) {
                processDefinitionMap.put(processDefinition.getId(), processDefinition);
            }
            for (ProcessInstanceResponse processInstanceResponse : processInstanceList) {
                if (processDefinitionMap.containsKey(processInstanceResponse.getProcessDefinitionId())) {
                    ProcessDefinition processDefinition = processDefinitionMap.get(processInstanceResponse.getProcessDefinitionId());
                    processInstanceResponse.setProcessDefinitionName(processDefinition.getName());
                    processInstanceResponse.setProcessDefinitionDescription(processDefinition.getDescription());
                }
            }
        }

        return responseList;

    }


    @ApiOperation(value = "获取流程实例列表分页接口", notes="内部查询BusinessProcess数据", tags = { "Process Instances" })
    @PostMapping(value = "/process-instances/instancePageList", produces = "application/json")
    public PageBean<ProcessInstanceModel> instancePageList(@RequestBody ProcessInstanceModel model/*,
               @ApiParam @RequestParam(defaultValue = "1") Integer startPage, @ApiParam @RequestParam(defaultValue = "10") Integer pageSize*/) {
        //PageRequest pageRequest = PageRequest.of(startPage-1, pageSize, Sort.by(Sort.Order.desc("startTime")));
        PageRequest pageRequest = PageBean.getPageRequest(model);
        return processInstanceService.findInstanceList(model,pageRequest);
    }


    @ApiOperation(value = "获取流程实例列表", notes="内部查询BusinessProcess数据", tags = { "Process Instances" })
    @PostMapping(value = "/process-instances/instanceList", produces = "application/json")
    public List<ProcessInstanceModel> instanceList(@RequestBody ProcessInstanceModel model) {
        return processInstanceService.findInstanceList(model);
    }


    @ApiOperation(value = "获取一个流程实例", notes="内部查询BusinessProcess数据", tags = { "Process Instances" })
    @GetMapping(value = "/process-instances/getProcessInstance", produces = "application/json")
    public ProcessInstanceModel getProcessInstance(@ApiParam(name = "processInstanceId",value = "流程实例ID") @RequestParam String processInstanceId) {
        return processInstanceService.getProcessInstance(processInstanceId);
    }


    @ApiOperation(value = "发起流程", tags = { "Process Instances" })
    @PostMapping(value = "/process-instances/startProcessInstance", produces = "application/json")
    public ProcessInstanceModel startProcessInstance(@RequestBody StartProcessRequest startRequest) {
        String processDefinitionId = startRequest.getProcessDefinitionId();
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(processDefinition==null) throw new WorkflowException("流程发起-流程定义不存在："+processDefinitionId);
        return processInstanceService.startProcess(processDefinition,startRequest);
    }


    @ApiOperation(value = "删除流程实例", tags = { "Process Instances" })
    @DeleteMapping(value = "/process-instances/{processInstanceId}")
    public void deleteProcessInstance( HttpServletResponse response,
            @ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestParam(value = "deleteReason", required = false) String deleteReason ) {
        processInstanceService.deleteProcessInstance(processInstanceId, deleteReason);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "删除多个流程实例", tags = { "Process Instances" })
    @DeleteMapping("/process-instances/batchDeleteInstances")
    public void batchDeleteInstances(HttpServletResponse response,
         @ApiParam(name = "processInstanceIds",value = "多个流程实例ID") @RequestParam String[] processInstanceIds,
         @ApiParam(name = "deleteReason",value = "删除原因") @RequestParam(required = false) String deleteReason) {
        for(String processInstanceId : processInstanceIds ){
            processInstanceService.deleteProcessInstance(processInstanceId,deleteReason);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "挂起流程实例", tags = { "Process Instances" })
    @GetMapping(value = "/process-instances/suspendProcessInstance")
    public void suspendProcessInstance( HttpServletResponse response,
          @ApiParam(name = "processInstanceId",value = "流程实例ID") @RequestParam String processInstanceId,
          @ApiParam(name = "suspend",value = "是否挂起") @RequestParam Boolean suspend) {
        processInstanceService.suspendProcessInstance(processInstanceId,suspend);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "流程图标记审批节点信息", notes="流程图标记审批节点信息", tags = { "Process Instances" })
    @GetMapping(value = "/process-instances/highlights", produces = "application/json")
    public Map<String,Object>  highlights(@ApiParam(name = "processInstanceId",value = "流程实例ID") @RequestParam String processInstanceId) {

        Map<String,Object> result = new HashMap<>();
        HistoricProcessInstance hisInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if(hisInstance==null) return result;

        List<Map<String,Object>> doneActivities = new ArrayList();
        List<Map<String,Object>> sequenceFlows = new ArrayList();
        //查询历史审批节点
        List<HistoricActivityInstance>   historicActivityInstances =    historyService.createHistoricActivityInstanceQuery().processInstanceId(processInstanceId).orderByHistoricActivityInstanceEndTime().asc().list();
        for(HistoricActivityInstance activity : historicActivityInstances){
            Map<String,Object> node = new HashMap<>();
            //相同节点只存一次
            /*for(Map<String,Object> nodeMap : doneActivities){
                if(nodeMap.get("activityId").toString().equals(activity.getActivityId())){
                    node = nodeMap;
                }
            }
            for(Map<String,Object> nodeMap : sequenceFlows){
                if(nodeMap.get("activityId").toString().equals(activity.getActivityId())){
                    node = nodeMap;
                }
            }*/
            //调用活动时
            String activityType = activity.getActivityType();
            if(activityType.equals(FlowElementType.CALL_ACTIVITY)){
                if(activity.getCalledProcessInstanceId()!=null){
                    List subProcess = node.containsKey("subProcess") ? (List) node.get("subProcess") : new ArrayList();
                    Map<String,Object> map = new HashMap<>();
                    HistoricProcessInstance subInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(activity.getCalledProcessInstanceId()).finished().singleResult();
                    map.put("processInstanceId",subInstance.getId());
                    map.put("processInstanceName",subInstance.getName());
                    subProcess.add(map);
                    node.put("subProcess",subProcess);
                }
            }
            String taskId = activity.getTaskId();
            //查询审批意见
            if(taskId!=null){
                List comments = node.containsKey("comments") ? (List) node.get("comments") : new ArrayList();
                for(Comment c : taskService.getTaskComments(taskId) ){
                    Map<String,Object> comment = new HashMap<>();
                    comment.put("id",c.getId());
                    comment.put("time",c.getTime());
                    comment.put("opinion",c.getFullMessage());
                    comments.add(comment);
                }
                node.put("comments",comments);
            }
            node.put("activityId",activity.getActivityId());
            node.put("activityName",activity.getActivityName());
            node.put("activityType",activityType);

            if(activityType.equals("sequenceFlow")){
                sequenceFlows.add(node);
            }else{
                node.put("endTime",activity.getEndTime());
                node.put("assignee",activity.getAssignee());
                doneActivities.add(node);
            }

        }

        Map<String,Object> processDefinition = new HashMap<>();
        processDefinition.put("processDefinitionId",hisInstance.getProcessDefinitionId());
        processDefinition.put("processDefinitionName",hisInstance.getProcessDefinitionName());
        result.put("processDefinition",processDefinition);

        result.put("doneActivities",doneActivities);
        result.put("sequenceFlows",sequenceFlows);

        return result;
    }





}
