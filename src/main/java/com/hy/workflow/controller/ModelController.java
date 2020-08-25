package com.hy.workflow.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.service.ModelService;
import com.hy.workflow.util.EntityModelUtil;
import io.swagger.annotations.*;
import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.common.rest.api.PaginateRequest;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.impl.ModelQueryProperty;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ModelQuery;
import org.flowable.rest.service.api.repository.ModelResponse;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.util.*;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;

@RestController
@RequestMapping("/ModelController")
@Api(value = "模型", tags = "Models", description = "流程模型接口")
public class ModelController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ModelService modelService;

    @Autowired
    private ObjectMapper objectMapper;

    private static Map<String, QueryProperty> allowedSortProperties = new HashMap<>();


    static {
        allowedSortProperties.put("id", ModelQueryProperty.MODEL_ID);
        allowedSortProperties.put("category", ModelQueryProperty.MODEL_CATEGORY);
        allowedSortProperties.put("createTime", ModelQueryProperty.MODEL_CREATE_TIME);
        allowedSortProperties.put("key", ModelQueryProperty.MODEL_KEY);
        allowedSortProperties.put("lastUpdateTime", ModelQueryProperty.MODEL_LAST_UPDATE_TIME);
        allowedSortProperties.put("name", ModelQueryProperty.MODEL_NAME);
        allowedSortProperties.put("version", ModelQueryProperty.MODEL_VERSION);
        allowedSortProperties.put("tenantId", ModelQueryProperty.MODEL_TENANT_ID);
    }


    @GetMapping(value = "/models", produces = "application/json")
    @ApiOperation(value = "获取流程模型列表",  tags = { "Models" })
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "category", dataType = "string" ,paramType = "query"),
            @ApiImplicitParam(name = "categoryLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "categoryNotEquals", dataType = "string",  paramType = "query"),
            @ApiImplicitParam(name = "name", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "nameLike", dataType = "string",  paramType = "query"),
            @ApiImplicitParam(name = "key", dataType = "string",  paramType = "query"),
            @ApiImplicitParam(name = "deploymentId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "version", dataType = "integer", paramType = "query"),
            @ApiImplicitParam(name = "latestVersion", dataType = "boolean",  paramType = "query"),
            @ApiImplicitParam(name = "deployed", dataType = "boolean",  paramType = "query"),
            @ApiImplicitParam(name = "tenantId", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "tenantIdLike", dataType = "string", paramType = "query"),
            @ApiImplicitParam(name = "withoutTenantId", dataType = "boolean",  paramType = "query"),
            @ApiImplicitParam(name = "sort", dataType = "string",  paramType = "query"),
    })
    public DataResponse<ModelResponse> getModels( HttpServletRequest request,
            @ApiParam @RequestParam(defaultValue = "1") Integer startPage,
            @ApiParam @RequestParam(defaultValue = "10") Integer pageSize,
            @ApiParam(hidden = true)@RequestParam Map<String, String> params ) {

        ModelQuery modelQuery = repositoryService.createModelQuery();
        if (params.containsKey("id") && StringUtils.isNotBlank(params.get("id")) ) {
            modelQuery.modelId(params.get("id"));
        }
        if (params.containsKey("category") && StringUtils.isNotBlank(params.get("category")) ) {
            modelQuery.modelCategory(params.get("category"));
        }
        if (params.containsKey("categoryLike") && StringUtils.isNotBlank(params.get("categoryLike"))) {
            modelQuery.modelCategoryLike(params.get("categoryLike"));
        }
        if (params.containsKey("categoryNotEquals") && StringUtils.isNotBlank(params.get("categoryNotEquals"))) {
            modelQuery.modelCategoryNotEquals(params.get("categoryNotEquals"));
        }
        if (params.containsKey("name") && StringUtils.isNotBlank(params.get("name")) ) {
            modelQuery.modelName(params.get("name"));
        }
        if (params.containsKey("nameLike") && StringUtils.isNotBlank(params.get("nameLike")) ) {
            modelQuery.modelNameLike(params.get("nameLike"));
        }
        if (params.containsKey("key") && StringUtils.isNotBlank(params.get("key")) ) {
            modelQuery.modelKey(params.get("key"));
        }
        if (params.containsKey("version") && StringUtils.isNotBlank(params.get("version")) ) {
            modelQuery.modelVersion(Integer.valueOf(params.get("version")));
        }
        if (params.containsKey("latestVersion") && StringUtils.isNotBlank(params.get("latestVersion")) ) {
            boolean isLatestVersion = Boolean.valueOf(params.get("latestVersion"));
            if (isLatestVersion) {
                modelQuery.latestVersion();
            }
        }
        if (params.containsKey("deploymentId") && StringUtils.isNotBlank(params.get("deploymentId")) ) {
            modelQuery.deploymentId(params.get("deploymentId"));
        }
        if (params.containsKey("deployed") && StringUtils.isNotBlank(params.get("deployed")) ) {
            boolean isDeployed = Boolean.valueOf(params.get("deployed"));
            if (isDeployed) {
                modelQuery.deployed();
            } else {
                modelQuery.notDeployed();
            }
        }
        if (params.containsKey("tenantId") && StringUtils.isNotBlank(params.get("tenantId")) ) {
            modelQuery.modelTenantId(params.get("tenantId"));
        }
        if (params.containsKey("tenantIdLike") && StringUtils.isNotBlank(params.get("tenantIdLike")) ) {
            modelQuery.modelTenantIdLike(params.get("tenantIdLike"));
        }
        if (params.containsKey("withoutTenantId") && StringUtils.isNotBlank(params.get("withoutTenantId")) ) {
            boolean withoutTenantId = Boolean.valueOf(params.get("withoutTenantId"));
            if (withoutTenantId) {
                modelQuery.modelWithoutTenantId();
            }
        }

        PaginateRequest paginateRequest = new PaginateRequest();
        paginateRequest.setStart( (startPage-1)*pageSize );
        paginateRequest.setSize(Integer.valueOf(params.get("pageSize")));

        return paginateList(params, paginateRequest, modelQuery,"id", allowedSortProperties, EntityModelUtil::toModelResponseList);

    }


    @ApiOperation(value = "创建或修改模型", tags = {"Models" })
    @PostMapping(value = "/models",consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE )
    public ModelResponse saveModel(@RequestBody MultiValueMap<String, String> paramMap) throws JsonProcessingException {

        String json = paramMap.getFirst("json_xml");
        String modelId = paramMap.getFirst("modelId");
        String modeltype = paramMap.getFirst("modeltype");
        String description = paramMap.getFirst("description");
        String name = paramMap.getFirst("name");

        ObjectNode editorJsonNode = (ObjectNode)this.objectMapper.readTree(json);
        ObjectNode propertiesNode = (ObjectNode)editorJsonNode.get("properties");
        String key = propertiesNode.get("process_id")==null?null: propertiesNode.get("process_id").asText();
        //String name = propertiesNode.get("name")==null?null: propertiesNode.get("name").asText();
        String acutor = propertiesNode.get("process_author")==null?null: propertiesNode.get("process_author").asText();
        //String documentation = propertiesNode.get("documentation")==null?null: propertiesNode.get("documentation").asText();

        if(StringUtils.isEmpty(name))  throw new WorkflowException("流程名称不能为空！");

        Model model =  repositoryService.newModel() ;

        /*新建模型*/
        if(StringUtils.isEmpty(modelId)||"-1".equals(modelId)){
            //填写Key时判断后台是否已经存在
            if(StringUtils.isNotBlank(key)){
                Model existModel = repositoryService.createModelQuery().modelKey(key).singleResult();
                if(existModel!=null)  throw new WorkflowException("流程标识为："+key+" 的模型已经存在！");
            }
            //未填写Key时自动设置模型ID为Key
            repositoryService.saveModel(model);
            if(StringUtils.isBlank(key)){
                key = "Model"+ model.getId();
                propertiesNode.put("process_id",key);
            }
        }
        /*修改模型*/
        else{
            model = repositoryService.createModelQuery().modelId(modelId).singleResult();
            if(StringUtils.isEmpty(key)) throw new WorkflowException("流程标识不能为空！");
            //key是否有修改
            if(!key.equals(model.getKey())){
                Model existModel = repositoryService.createModelQuery().modelKey(key).singleResult();
                if(existModel!=null)  throw new WorkflowException("流程标识为："+key+" 的模型已经存在！");
            }
        }

        model.setKey(key);
        model.setName(name);
        propertiesNode.put("name",name);

        //设置MetaInfo信息
        ObjectNode metaInfoNode =this.objectMapper.createObjectNode();
        ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
        ObjectNode propNode = this.objectMapper.createObjectNode();
        metaInfoNode.put("id", model.getId());
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        metaInfoNode.set("stencilset", stencilSetNode);
        metaInfoNode.put("process_id", key);
        propNode.put("name", name);
        propNode.put("model_type", modeltype);
        propNode.put("process_author", acutor);
        propNode.put("description", description);
        //单位部门信息需要更新
        propNode.put("department_id", "1000012");
        propNode.put("unit_id", "1000000");
        metaInfoNode.set("properties", propNode);
        model.setMetaInfo(metaInfoNode.toString());

        repositoryService.saveModel(model);
        editorJsonNode.put("modelId",model.getId());
        repositoryService.addModelEditorSource(model.getId(), editorJsonNode.toString().getBytes());

        //模型记录信息
        //模型ID、Key、名称、版本、创建人、创建部门、创建单位、创建时间、修改时间

        return EntityModelUtil.toModelResponse(model);
    }


    @ApiOperation(value = "部署模型", tags = { "Models" })
    @PutMapping(value = "/models/{modelId}")
    public void deploy(@ApiParam(name = "modelId",value = "模型ID") @PathVariable("modelId") String modelId, HttpServletResponse response) {
        Model model = repositoryService.getModel(modelId);
        modelService.deploy(model);
        response.setStatus(HttpStatus.OK.value());
        //return  new ResponseEntity<>( "OK",HttpStatus.OK);
    }


    @ApiOperation(value = "根据模型ID查找模型", tags = { "Models" })
    @GetMapping(value = "/models/{modelId}", produces = "application/json")
    public ModelResponse getModel(@ApiParam(name = "modelId",value = "模型ID") @PathVariable String modelId, HttpServletRequest request) {
        Model model = repositoryService.createModelQuery().modelId(modelId).singleResult();
        return EntityModelUtil.toModelResponse(model);
    }


    @ApiOperation(value = "删除一个模型", tags = { "Models" })
    @DeleteMapping("/models/{modelId}")
    public void deleteModel(@ApiParam(name = "modelId",value = "模型ID") @PathVariable String modelId, HttpServletResponse response) {
        Model model = repositoryService.createModelQuery().modelId(modelId).singleResult();
        repositoryService.deleteModel(model.getId());
        response.setStatus(HttpStatus.NO_CONTENT.value());

    }


    @ApiOperation(value = "删除多个模型", tags = { "Models" })
    @DeleteMapping("/models/batchDeleteModel")
    public void batchDeleteModel(@ApiParam(name = "modelIds",value = "模型ID") @RequestParam String[] modelIds, HttpServletResponse response) {
        for(String modelId : modelIds){
            repositoryService.deleteModel(modelId);
        }
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "模型编辑器编辑模型",ignoreJsonView = true,hidden = true)
    @GetMapping(value = {"/models/{modelId}/editor/json"},produces = {"application/json"})
    public ObjectNode getModelJSON(@PathVariable String modelId) throws JsonProcessingException {
        Model model = repositoryService.createModelQuery().modelId(modelId).singleResult();
        ObjectNode modelNode = this.objectMapper.createObjectNode();
        modelNode.put("modelId", model.getId());
        modelNode.put("name", model.getName());
        modelNode.put("key", model.getKey());

        modelNode.putPOJO("lastUpdated", model.getCreateTime());
        modelNode.put("lastUpdatedBy", model.getCreateTime().toString());
        ObjectNode editorJsonNode;
        if (model.hasEditorSource()) {
            try {
                String source = new String(repositoryService.getModelEditorSource(model.getId()));
                editorJsonNode = (ObjectNode)this.objectMapper.readTree(source);
                editorJsonNode.put("modelType", "model");
                editorJsonNode.put("process_id", model.getKey());
                modelNode.set("model", editorJsonNode);
            } catch (Exception var6) {
                throw new InternalServerErrorException("Error reading editor json " );
            }
        } else {
            editorJsonNode = this.objectMapper.createObjectNode();
            editorJsonNode.put("id", "canvas");
            editorJsonNode.put("resourceId", "canvas");
            ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
            stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
            editorJsonNode.put("modelType", "model");
            modelNode.set("model", editorJsonNode);
        }

        TextNode description = (TextNode)this.objectMapper.readTree(model.getMetaInfo()).get("properties").get("description");
        if(description!=null) modelNode.put("description",description.asText());

        return modelNode;
    }


    @ApiOperation(value = "模型编辑器新建模型",ignoreJsonView = true,hidden = true)
    @GetMapping(value = {"/models/newModelJson"},produces = {"application/json"})
    public ObjectNode getNewModelJSON() {
        ObjectNode modelNode = this.objectMapper.createObjectNode();
        modelNode.put("modelId","-1");
        modelNode.put("name", "");
        modelNode.put("key", "NewModel");
        modelNode.putPOJO("lastUpdated", new Date());
        ObjectNode editorJsonNode;
        editorJsonNode = this.objectMapper.createObjectNode();
        editorJsonNode.put("id", "canvas");
        editorJsonNode.put("resourceId", "canvas");
        ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        stencilSetNode.put("url", "../editor/stencilsets/bpmn2.0/bpmn2.0.json");
        editorJsonNode.put("modelType", "model");
        editorJsonNode.set("stencilset", stencilSetNode);
        ObjectNode stencil = this.objectMapper.createObjectNode();
        stencil.put("id","BPMNDiagram");
        editorJsonNode.set("stencil",stencil);
        ObjectNode propertiesNode = this.objectMapper.createObjectNode();
        propertiesNode.put("process_id", "");
        propertiesNode.put("name", "");
        propertiesNode.put("process_namespace", "http://www.flowable.org/processdef");
        propertiesNode.put("process_author", "赵耀");
        propertiesNode.put("isexecutable", true);
        editorJsonNode.set("properties",propertiesNode);
        modelNode.set("model", editorJsonNode);
        return modelNode;
    }


    @ApiOperation(value = "获取所有流程模型",tags = {"Models"})
    @GetMapping(value = "/allModels", produces = "application/json")
    public DataResponse<ModelResponse>  getAllModels( HttpServletRequest request) {
        ModelQuery modelQuery = repositoryService.createModelQuery();
        List<Model> allModels = modelQuery.list();
        DataResponse<ModelResponse> dataResponse = new DataResponse();
        dataResponse.setSize(allModels.size());
        dataResponse.setData(EntityModelUtil.toModelResponseList(allModels));
        dataResponse.setTotal(allModels.size());
        return dataResponse;
    }



}
