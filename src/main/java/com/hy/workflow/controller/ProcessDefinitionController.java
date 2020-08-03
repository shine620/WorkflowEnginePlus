package com.hy.workflow.controller;

import com.hy.workflow.util.EntityModelUtil;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.common.rest.resolver.ContentTypeResolver;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.ProcessDefinitionQueryProperty;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

@RestController
@RequestMapping("/ProcessDefinitionController")
@Api(value = "流程定义", tags = "Process Definitions", description = "流程定义接口")
public class ProcessDefinitionController {


    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_ID);
        properties.put("key", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
        properties.put("category", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_CATEGORY);
        properties.put("name", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_NAME);
        properties.put("version", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_VERSION);
        properties.put("deploymentId", ProcessDefinitionQueryProperty.DEPLOYMENT_ID);
        properties.put("tenantId", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_TENANT_ID);
    }

    @ApiOperation(value = "List of process definitions", tags = { "Process Definitions" }, nickname = "listProcessDefinitions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "version", dataType = "integer",paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string",  paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "keyLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "resourceName", dataType = "string",paramType = "query"),
            @ApiImplicitParam(name = "resourceNameLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "startableByUser", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "latest", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "suspended", dataType = "boolean", paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string", allowableValues = "name,id,key,category,deploymentId,version", paramType = "query"),
    })
    @GetMapping(value = "/process-definitions", produces = "application/json")
    public DataResponse<ProcessDefinitionResponse> getProcessDefinitions(HttpServletRequest request,
            @ApiParam @RequestParam(defaultValue = "1") Integer startPage,
            @ApiParam @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(hidden = true) @RequestParam Map<String, String> params) {

        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();

        if (params.containsKey("category") && StringUtils.isNotBlank(params.get("category")) ) {
            processDefinitionQuery.processDefinitionCategory(params.get("category"));
        }
        if (params.containsKey("categoryLike") && StringUtils.isNotBlank(params.get("categoryLike"))) {
            processDefinitionQuery.processDefinitionCategoryLike(params.get("categoryLike"));
        }
        if (params.containsKey("categoryNotEquals") && StringUtils.isNotBlank(params.get("categoryNotEquals"))) {
            processDefinitionQuery.processDefinitionCategoryNotEquals(params.get("categoryNotEquals"));
        }
        if (params.containsKey("key") && StringUtils.isNotBlank(params.get("key"))) {
            processDefinitionQuery.processDefinitionKey(params.get("key"));
        }
        if (params.containsKey("keyLike") && StringUtils.isNotBlank(params.get("keyLike"))) {
            processDefinitionQuery.processDefinitionKeyLike(params.get("keyLike"));
        }
        if (params.containsKey("name") && StringUtils.isNotBlank(params.get("name"))) {
            processDefinitionQuery.processDefinitionName(params.get("name"));
        }
        if (params.containsKey("nameLike") && StringUtils.isNotBlank(params.get("nameLike"))) {
            processDefinitionQuery.processDefinitionNameLike(params.get("nameLike"));
        }
        if (params.containsKey("resourceName") && StringUtils.isNotBlank(params.get("resourceName"))) {
            processDefinitionQuery.processDefinitionResourceName(params.get("resourceName"));
        }
        if (params.containsKey("resourceNameLike") && StringUtils.isNotBlank(params.get("resourceNameLike"))) {
            processDefinitionQuery.processDefinitionResourceNameLike(params.get("resourceNameLike"));
        }
        if (params.containsKey("version") && StringUtils.isNotBlank(params.get("version"))) {
            processDefinitionQuery.processDefinitionVersion(Integer.valueOf(params.get("version")));
        }
        if (params.containsKey("suspended") && StringUtils.isNotBlank(params.get("suspended"))) {
            Boolean suspended = Boolean.valueOf(params.get("suspended"));
            if (suspended != null) {
                if (suspended) {
                    processDefinitionQuery.suspended();
                } else {
                    processDefinitionQuery.active();
                }
            }
        }
        //默认查询最新版本流程定义
        /*if(!params.containsKey("latest") || StringUtils.isBlank(params.get("latest"))  ){
            processDefinitionQuery.latestVersion();
        } else{
            Boolean latest = Boolean.valueOf(params.get("latest"));
            if (latest != null && latest) {
                processDefinitionQuery.latestVersion();
            }
        }*/
        if (params.containsKey("deploymentId") && StringUtils.isNotBlank(params.get("deploymentId"))) {
            processDefinitionQuery.deploymentId(params.get("deploymentId"));
        }
        if (params.containsKey("startableByUser") && StringUtils.isNotBlank(params.get("startableByUser"))) {
            processDefinitionQuery.startableByUser(params.get("startableByUser"));
        }
        if (params.containsKey("tenantId") && StringUtils.isNotBlank(params.get("tenantId"))) {
            processDefinitionQuery.processDefinitionTenantId(params.get("tenantId"));
        }
        if (params.containsKey("tenantIdLike") && StringUtils.isNotBlank(params.get("tenantIdLike"))) {
            processDefinitionQuery.processDefinitionTenantIdLike(params.get("tenantIdLike"));
        }

        PaginateRequest paginateRequest = new PaginateRequest();
        paginateRequest.setStart( (startPage-1)*pageSize );
        paginateRequest.setSize(Integer.valueOf(params.get("pageSize")));

        return paginateList(params, paginateRequest, processDefinitionQuery,"id", properties, EntityModelUtil::toProcessDefinitionResponseList);

    }


    @ApiOperation(value = "Delete a Process Definitions", notes="删除一个流程定义",tags = { "Process Definitions" })
    @DeleteMapping("/process-definitions/{deploymentId}")
    public void deleteModel(@ApiParam(name = "deploymentId",value = "部署ID") @PathVariable String deploymentId, HttpServletResponse response) {
        //不带级联的删除，只能删除没有启动的流程，如果流程启动，就会抛出异常
        repositoryService.deleteDeployment(deploymentId);
        //能级联的删除，能删除启动的流程，会删除和当前规则相关的所有信息，正在执行的信息，也包括历史信息
        //repositoryService.deleteDeployment(deploymentId,true);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "Batch Delete Process Definitions", notes="删除多个流程定义",tags = { "Process Definitions" })
    @DeleteMapping("/process-definitions/batchProcessDefinitions")
    public void batchDeleteModel(@ApiParam(name = "deploymentIds",value = "部署ID") @RequestParam String[] deploymentIds, HttpServletResponse response) {
        for(String deploymentId : deploymentIds){
            repositoryService.deleteDeployment(deploymentId);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "Suspend Process Definitions", notes="挂起一个流程定义",tags = { "Process Definitions" })
    @PutMapping("/process-definitions/suspend/{processDefinitionId}")
    public void suspendProcessDefinition(@ApiParam(name = "processDefinitionId",value = "流程定义ID") @PathVariable String processDefinitionId, HttpServletResponse response) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(processDefinition.isSuspended()){
            repositoryService.activateProcessDefinitionById(processDefinitionId);
        }else{
            repositoryService.suspendProcessDefinitionById(processDefinitionId);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "Get a process definition resource content",notes="查看流程定义资源文件", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/xml/{processDefinitionId}")
    public void getProcessDefinitionResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletResponse response) throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        byte[] resourceData= getDeploymentResourceData(processDefinition.getDeploymentId(), processDefinition.getResourceName(), response);
        response.getOutputStream().write(resourceData);
    }


    @ApiOperation(value = "Get a diagram resource content",notes="查看系统生成的流程图片", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/png/{processDefinitionId}")
    public void getDiagramResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletResponse response) throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        byte[] resourceData= getDeploymentResourceData(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName(), response);
        response.getOutputStream().write(resourceData);
    }


    protected byte[] getDeploymentResourceData(String deploymentId, String resourceName, HttpServletResponse response) {

        if (deploymentId == null) {
            throw new FlowableIllegalArgumentException("No deployment id provided");
        }
        if (resourceName == null) {
            throw new FlowableIllegalArgumentException("No resource name provided");
        }

        Deployment deployment = repositoryService.createDeploymentQuery().deploymentId(deploymentId).singleResult();
        if (deployment == null) {
            throw new FlowableObjectNotFoundException("Could not find a deployment with id '" + deploymentId + "'.", Deployment.class);
        }

        List<String> resourceList = repositoryService.getDeploymentResourceNames(deploymentId);

        if (resourceList.contains(resourceName)) {
            final InputStream resourceStream = repositoryService.getResourceAsStream(deploymentId, resourceName);

            String contentType = contentTypeResolver.resolveContentType(resourceName);
            response.setContentType(contentType);
            try {
                return IOUtils.toByteArray(resourceStream);
            } catch (Exception e) {
                throw new FlowableException("Error converting resource stream", e);
            }
        } else {
            throw new FlowableObjectNotFoundException("Could not find a resource with name '" + resourceName + "' in deployment '" + deploymentId + "'.", String.class);
        }

    }




}
