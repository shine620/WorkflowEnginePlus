package com.hy.workflow.util;


import org.flowable.engine.repository.Model;
import org.flowable.rest.service.api.repository.ModelResponse;

import java.util.ArrayList;
import java.util.List;

public class ProcessUtil {

    public static List<ModelResponse> toModelResponseList(List<Model> models){

        List<ModelResponse> list = new ArrayList<ModelResponse>();
        for(Model model : models){
            list.add(toModelResponse(model));
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

}
