package com.hy.workflow.service;

import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.model.ProcessDefinitionConfigModel;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;

@Service
public class ProcessDefinitionConfigService {

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;

    @Transactional
    public ProcessDefinitionConfigModel saveProcessDefinitionConfig(ProcessDefinitionConfig config) {
        ProcessDefinitionConfig processDefinitionConfig = processDefinitionConfigRepository.save(config);
        ProcessDefinitionConfigModel pdConfigModel = new ProcessDefinitionConfigModel();
        BeanUtils.copyProperties(processDefinitionConfig,pdConfigModel);
        return  pdConfigModel;
    }



}
