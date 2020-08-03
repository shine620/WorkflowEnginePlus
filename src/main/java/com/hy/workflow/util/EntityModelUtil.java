package com.hy.workflow.util;


import org.flowable.common.rest.util.RestUrlBuilder;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.rest.service.api.RestUrls;
import org.flowable.rest.service.api.repository.ModelResponse;
import org.flowable.rest.service.api.repository.ProcessDefinitionResource;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;

import java.util.ArrayList;
import java.util.List;

public class EntityModelUtil {

    public static List<ModelResponse> toModelResponseList(List<Model> models){
        List<ModelResponse> list = new ArrayList<ModelResponse>();
        for(Model model : models){
            list.add(toModelResponse(model));
        }
        return list;
    }

    public static List<ProcessDefinitionResponse> toProcessDefinitionResponseList(List<ProcessDefinition> definitions){
        List<ProcessDefinitionResponse> list = new ArrayList<ProcessDefinitionResponse>();
        for(ProcessDefinition d : definitions){
            list.add(toProcessDefinitionResponse(d));
        }
        return list;
    }

    public static ModelResponse toModelResponse(Model model){
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
        //RestUrlBuilder urlBuilder = RestUrlBuilder.fromCurrentRequest();
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
        /*pdResponse.setUrl(urlBuilder.buildUrl(RestUrls.URL_PROCESS_DEFINITION, pd.getId()));
        pdResponse.setDeploymentUrl(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT, pd.getDeploymentId()));
        pdResponse.setResource(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, pd.getDeploymentId(), pd.getResourceName()));
        if (pd.getDiagramResourceName() != null) {
            pdResponse.setDiagramResource(urlBuilder.buildUrl(RestUrls.URL_DEPLOYMENT_RESOURCE, pd.getDeploymentId(), pd.getDiagramResourceName()));
        }*/
        return pdResponse;
    }

}
