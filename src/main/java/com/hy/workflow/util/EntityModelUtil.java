package com.hy.workflow.util;


import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.ProcessDefinitionConfigModel;
import com.hy.workflow.model.ProcessInstanceModel;
import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.repository.ModelResponse;
import org.flowable.rest.service.api.repository.ProcessDefinitionResource;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.beans.BeanUtils;

import java.util.ArrayList;
import java.util.List;

public class EntityModelUtil {

    public static List<ModelResponse> toModelResponseList(List<Model> models){
        if(models==null) return null;
        List<ModelResponse> list = new ArrayList<ModelResponse>();
        for(Model model : models){
            list.add(toModelResponse(model));
        }
        return list;
    }

    public static List<ProcessDefinitionResponse> toProcessDefinitionResponseList(List<ProcessDefinition> definitions){
        if(definitions==null) return null;
        List<ProcessDefinitionResponse> list = new ArrayList<ProcessDefinitionResponse>();
        for(ProcessDefinition d : definitions){
            list.add(toProcessDefinitionResponse(d));
        }
        return list;
    }

    public static ModelResponse toModelResponse(Model model){
        if(model==null) return null;
        ModelResponse modelResponse = new ModelResponse();
        modelResponse.setCategory(model.getCategory());
        modelResponse.setCreateTime(model.getCreateTime());
        modelResponse.setId(model.getId());
        modelResponse.setKey(model.getKey());
        modelResponse.setLastUpdateTime(model.getLastUpdateTime());
        modelResponse.setMetaInfo(model.getMetaInfo());
        modelResponse.setName(model.getName());
        modelResponse.setDeploymentId(model.getDeploymentId());
        modelResponse.setVersion(model.getVersion());
        modelResponse.setTenantId(model.getTenantId());
        return modelResponse;
    }

    public static ProcessDefinitionResponse toProcessDefinitionResponse(ProcessDefinition pd){
        if(pd==null) return null;
        ProcessDefinitionResponse pdResponse = new ProcessDefinitionResponse();
        pdResponse.setId(pd.getId());
        pdResponse.setName(pd.getName());
        pdResponse.setKey(pd.getKey());
        pdResponse.setTenantId(pd.getTenantId());
        pdResponse.setVersion(pd.getVersion());
        pdResponse.setCategory(pd.getCategory());
        pdResponse.setDeploymentId(pd.getDeploymentId());
        pdResponse.setDescription(pd.getDescription());
        pdResponse.setSuspended(pd.isSuspended());
        pdResponse.setStartFormDefined(pd.hasStartFormKey());
        pdResponse.setGraphicalNotationDefined(pd.hasGraphicalNotation());
        return pdResponse;
    }

    public static ProcessDefinitionConfigModel toProcessDefinitionConfigModel(ProcessDefinitionConfig pdConfig) {
        if(pdConfig!=null){
            ProcessDefinitionConfigModel pdConfigModel = new ProcessDefinitionConfigModel();
            BeanUtils.copyProperties(pdConfig,pdConfigModel);
            return  pdConfigModel;
        }
        return null;
    }

    public static FlowElementConfigModel toFlowElementConfigMode(FlowElementConfig feConfig) {
        if(feConfig!=null){
            FlowElementConfigModel model = new FlowElementConfigModel();
            BeanUtils.copyProperties(feConfig,model);
            return  model;
        }
        return null;
    }

    public static List<ProcessInstanceResponse> toProcessInstanceResponseList(List<ProcessInstance> reqs) {
        if(reqs==null) return null;
        List<ProcessInstanceResponse> list = new ArrayList<ProcessInstanceResponse>();
        for(ProcessInstance instance : reqs){
            list.add(toProcessInstanceResponse(instance));
        }
        return list;
    }

    public static ProcessInstanceResponse toProcessInstanceResponse(ProcessInstance instance){
        if(instance==null) return null;
        ProcessInstanceResponse instanceResponse = new ProcessInstanceResponse();
        instanceResponse.setBusinessKey(instance.getBusinessKey());
        instanceResponse.setId(instance.getId());
        instanceResponse.setName(instance.getName());
        instanceResponse.setProcessDefinitionId(instance.getProcessDefinitionId());
        instanceResponse.setProcessDefinitionName(instance.getProcessDefinitionName());
        instanceResponse.setStartTime(instance.getStartTime());
        instanceResponse.setStartUserId(instance.getStartUserId());
        instanceResponse.setSuspended(instance.isSuspended());
        return instanceResponse;
    }

    public static ProcessInstanceModel toProcessInstanceModel(ProcessInstanceModel model, ProcessInstance instance){
        if(instance!=null) {
            model.setProcessInstanceId(instance.getId());
            model.setProcessInstanceName(instance.getName());
            model.setBusinessKey(instance.getBusinessKey());
            model.setProcessDefinitionId(instance.getProcessDefinitionId());
            model.setProcessDefinitionName(instance.getProcessDefinitionName());
            model.setProcessDefinitionKey(instance.getProcessDefinitionKey());
            model.setEnded(false);
            model.setSuspended(instance.isSuspended());
            model.setStartTime(instance.getStartTime());
            model.setStartUserId(instance.getStartUserId());
            model.setDeploymentId(instance.getDeploymentId());
        }
        return model;
    }

    public static ProcessInstanceModel toProcessInstanceModel(BusinessProcess bp){
        if(bp==null) return null;
        ProcessInstanceModel model = new ProcessInstanceModel();
        BeanUtils.copyProperties(bp,model);
        return model;
    }


}
