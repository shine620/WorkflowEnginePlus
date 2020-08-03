package com.hy.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.IOException;
import java.util.Date;


/**
 * 模型Service
 * @author zhoayao
 * @version 1.0
 *
 */
@Service
@Transactional
public class ModelService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;


    /**
     * 部署模型
     *
     * @author:  zhaoyao
     * @param:  model 模型实体
     * @return: Model
     */
    public void deploy(Model model) {

        ObjectNode modelNode;
        try {
            modelNode = (ObjectNode) new ObjectMapper().readTree(repositoryService.getModelEditorSource(model.getId()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        BpmnModel bpmnModel = new BpmnJsonConverter().convertToBpmnModel(modelNode);
        byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);

        String processName = model.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment().name(model.getName()).addString(processName, new String(bpmnBytes)).deploy();

        //生成流程定义配置信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        ProcessDefinitionConfig pdConfig = new ProcessDefinitionConfig();
        pdConfig.setProcessDefinitionId(processDefinition.getId());
        pdConfig.setProcessDefinitionKey(processDefinition.getKey());
        pdConfig.setProcessDefinitionName(processDefinition.getName());
        pdConfig.setVersion(processDefinition.getVersion());
        pdConfig.setCreateTime(new Date());
        pdConfig.setUpdateTime(new Date());
        processDefinitionConfigRepository.save(pdConfig);

    }


}
