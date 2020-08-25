package com.hy.workflow.controller;

import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.model.ProcessInstanceModel;
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
import org.flowable.engine.impl.ProcessInstanceQueryProperty;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstanceQuery;
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
    public PageBean<ProcessInstanceModel> instanceList(@RequestBody ProcessInstanceModel model,
            @ApiParam @RequestParam(defaultValue = "1") Integer startPage, @ApiParam @RequestParam(defaultValue = "10") Integer pageSize) {
        PageRequest pageRequest = PageRequest.of(startPage-1, pageSize, Sort.by(Sort.Order.desc("startTime")));
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
    public ProcessInstanceModel startProcessInstance(
            @ApiParam(name = "processDefinitionId",value = "流程定义ID") @RequestParam String processDefinitionId,
            @ApiParam(name = "startUserId",value = "发起人ID") @RequestParam String startUserId,
            @ApiParam(name = "businessId",value = "业务ID") @RequestParam String businessId,
            @ApiParam(name = "businessType",value = "业务类型") @RequestParam  String businessType,
            @ApiParam(name = "businessName",value = "业务名称") @RequestParam(required = false) String businessName,
            @ApiParam(name = "businessUrl",value = "业务地址") @RequestParam(required = false) String businessUrl,
            @ApiParam(name = "variables",value = "流程变量") @RequestBody Map<String,Object> variables) {

        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(processDefinition==null) throw new WorkflowException("流程发起-流程定义不存在："+processDefinitionId);
        return processInstanceService.startProcess(processDefinition,startUserId,businessId,businessType,businessName,businessUrl,variables);
    }


    @ApiOperation(value = "删除流程实例", tags = { "Process Instances" })
    @DeleteMapping(value = "/process-instances/{processInstanceId}")
    public void deleteProcessInstance( HttpServletResponse response,
            @ApiParam(name = "processInstanceId") @PathVariable String processInstanceId,
            @RequestParam(value = "deleteReason", required = false) String deleteReason ) {
        processInstanceService.deleteProcessInstance(processInstanceId, deleteReason);
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



}
