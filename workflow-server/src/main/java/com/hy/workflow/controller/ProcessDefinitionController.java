package com.hy.workflow.controller;

import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.ProcessDefinitionConfigModel;
import com.hy.workflow.service.ProcessDefinitionService;
import com.hy.workflow.util.EntityModelUtil;
import io.swagger.annotations.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
@CrossOrigin
@Api(value = "流程定义", tags = "Process Definitions", description = "流程定义接口")
public class ProcessDefinitionController {


    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    protected ContentTypeResolver contentTypeResolver;

    @Autowired
    protected ProcessDefinitionService processDefinitionService;


    @ApiOperation(value = "查询流程定义列表",  nickname = "processDefinitions")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", paramType = "query"),
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
            @ApiImplicitParam(name = "sort", dataType = "string", allowableValues = "deploymentId,name,id,key,category,deploymentId,version", paramType = "query"),
    })
    @GetMapping(value = "/process-definitions/processDefinitions", produces = "application/json")
    public DataResponse<ProcessDefinitionResponse> getProcessDefinitions(HttpServletRequest request,
            @ApiParam @RequestParam(defaultValue = "1") Integer startPage,
            @ApiParam @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(hidden = true) @RequestParam Map<String, String> params) {

        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();
        if (params.containsKey("id") && StringUtils.isNotBlank(params.get("id")) ) {
            processDefinitionQuery.processDefinitionId(params.get("id"));
        }
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
        if(params.containsKey("latest")){
            Boolean latest = Boolean.valueOf(params.get("latest"));
            if (latest != null && latest) {
                processDefinitionQuery.latestVersion();
            }
        }
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
        paginateRequest.setOrder("desc");

        Map<String, QueryProperty> properties = new HashMap<>();
        properties.put("id", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_ID);
        properties.put("key", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_KEY);
        properties.put("category", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_CATEGORY);
        properties.put("name", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_NAME);
        properties.put("version", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_VERSION);
        properties.put("deploymentId", ProcessDefinitionQueryProperty.DEPLOYMENT_ID);
        properties.put("tenantId", ProcessDefinitionQueryProperty.PROCESS_DEFINITION_TENANT_ID);

        return paginateList(params, paginateRequest, processDefinitionQuery,"deploymentId", properties, EntityModelUtil::toProcessDefinitionResponseList);

    }


    @ApiOperation(value = "流程定义配置列表(多条件查询)", tags = { "Process Definitions" })
    @PostMapping("/process-definitions/definitionConfigs")
    public PageBean<ProcessDefinitionConfigModel> listProcessDefinitionConfig(@RequestBody ProcessDefinitionConfigModel configModel) {
        PageRequest pageRequest = PageBean.getPageRequest(configModel);
        return processDefinitionService.findProcessDefinitionConfigList(configModel,pageRequest);
    }


    @ApiOperation(value = "流程定义配置列表(多条件查询-不分页)", tags = { "Process Definitions" })
    @PostMapping("/process-definitions/findProcessDefinitionConfigs")
    public List<ProcessDefinitionConfigModel> findProcessDefinitionConfigs(@RequestBody ProcessDefinitionConfigModel configModel) {
        return processDefinitionService.findProcessDefinitionConfigList(configModel);
    }


    @ApiOperation(value = "删除流程部署", tags = { "Process Definitions" })
    @DeleteMapping("/deployments/{deploymentId}")
    public void deleteDeployment( HttpServletResponse response,
            @ApiParam(name = "deploymentId",value = "部署ID") @PathVariable String deploymentId,
            @ApiParam(name="cascade",value = "是否级联删除流程信息") @RequestParam(defaultValue = "false") Boolean cascade) {
        processDefinitionService.deleteDeployment(deploymentId,cascade);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "批量删除流程部署", tags = { "Process Definitions" })
    @DeleteMapping("/deployments/batchDeleteDeployments")
    public void batchDeleteDeployment(HttpServletResponse response,
            @ApiParam(name = "deploymentIds",value = "多个部署ID") @RequestParam String[] deploymentIds,
            @ApiParam(name="cascade",value = "是否级联删除流程信息") @RequestParam(defaultValue = "false") Boolean cascade) {
        processDefinitionService.deleteDeployments(deploymentIds,cascade);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "挂起流程定义", tags = { "Process Definitions" })
    @PutMapping("/process-definitions/suspend/{processDefinitionId}")
    public void suspendProcessDefinition(HttpServletResponse response,
            @ApiParam(name = "processDefinitionId",value = "流程定义ID") @PathVariable String processDefinitionId,
            @ApiParam(name = "suspend",value = "是否挂起") @RequestParam Boolean suspend) {
        processDefinitionService.suspendProcessDefinitionById(processDefinitionId,suspend);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "批量挂起流程定义", tags = { "Process Definitions" })
    @PostMapping(value = "/process-definitions/batchSuspend/processDefinitionIds",consumes = "application/json")
    //请求参数数据格式：{"processDefinitionIds":["1201","1202"],"suspend":true}
    public void batchSuspendProcessDefinition(HttpServletResponse response,@RequestBody Map content) {
        List<String> processDefinitionIds = (List<String>) content.get("processDefinitionIds");  //流程定义ID集合(processDefinitionIds)
        Boolean suspend = Boolean.valueOf(content.get("suspend").toString()); //是否挂起(suspend)
        if(processDefinitionIds!=null&&suspend!=null){
            for(String id : processDefinitionIds){
                processDefinitionService.suspendProcessDefinitionById(id,suspend);
            }
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }

    @ApiOperation(value = "查看流程定义资源文件", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/xml/{processDefinitionId}")
    public void getProcessDefinitionResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletResponse response) throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(processDefinition==null) throw new WorkflowException("资源文件查看-流程实例不存在："+processDefinitionId);
        byte[] resourceData= getDeploymentResourceData(processDefinition.getDeploymentId(), processDefinition.getResourceName(), response);
        response.getOutputStream().write(resourceData);
    }


    @ApiOperation(value = "查看系统生成的流程图片", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/png/{processDefinitionId}")
    public void getDiagramResource(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId, HttpServletResponse response) throws IOException {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(processDefinition==null) throw new WorkflowException("流程图片查看-流程实例不存在："+processDefinitionId);
        byte[] resourceData= getDeploymentResourceData(processDefinition.getDeploymentId(), processDefinition.getDiagramResourceName(), response);
        response.getOutputStream().write(resourceData);
    }


    @ApiOperation(value = "查询流程配置", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/getProcessConfig/{processDefinitionId}", produces = "application/json")
    public ProcessDefinitionConfigModel getProcessConfig(@ApiParam(name = "processDefinitionId",value = "流程定义ID") @PathVariable String processDefinitionId) {
        ProcessDefinitionConfigModel pdcModel = processDefinitionService.getProcessConfig(processDefinitionId);
        return pdcModel;
    }


    @ApiOperation(value = "保存流程配置", tags = { "Process Definitions" } )
    @PostMapping(value = "/process-definitions/saveProcessConfig", produces = "application/json")
    @ResponseBody
    public ProcessDefinitionConfigModel saveProcessConfig(@RequestBody ProcessDefinitionConfigModel pdConfigModel) {
        ProcessDefinitionConfigModel pdcModel = processDefinitionService.saveProcessConfig(pdConfigModel);
        return pdcModel;
    }


    @ApiOperation(value = "保存节点配置", tags = { "Process Definitions" } )
    @PostMapping(value = "/process-definitions/saveElementConfig", produces = "application/json")
    @ResponseBody
    public FlowElementConfigModel saveElementConfig(@RequestBody FlowElementConfigModel model) {
        FlowElementConfigModel eleModel = processDefinitionService.saveElementConfig(model);
        return eleModel;
    }


    @ApiOperation(value = "查询节点配置", tags = { "Process Definitions" })
    @GetMapping(value = "/process-definitions/getElementConfig", produces = "application/json")
    public FlowElementConfigModel getElementConfig(
            @ApiParam(name = "processDefinitionId",value = "流程定义ID") @RequestParam String processDefinitionId,
            @ApiParam(name = "flowElementId",value = "任务节点ID") @RequestParam String flowElementId) {
        FlowElementConfigModel eleModel = processDefinitionService.getFlowElementConfig(processDefinitionId,flowElementId);
        return eleModel;
    }


    @ApiOperation(value = "根据业务类型查询流程定义列表", tags = { "Process Definitions" })
    @PostMapping("/process-definitions/findProcessDefinitions")
    public List<ProcessDefinitionConfigModel> findProcessDefinitions(@RequestBody Map params) {
        return  processDefinitionService.findProcessDefinitions(params);
    }


    protected byte[] getDeploymentResourceData(String deploymentId, String resourceName, HttpServletResponse response) {
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
                throw new RuntimeException("Error converting resource stream", e);
            }
        } else {
            throw new FlowableObjectNotFoundException("Could not find a resource with name '" + resourceName + "' in deployment '" + deploymentId + "'.", String.class);
        }

    }


}
