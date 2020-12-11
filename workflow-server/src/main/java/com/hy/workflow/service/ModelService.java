package com.hy.workflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import com.hy.workflow.util.EntityModelUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.Process;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.Model;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.rest.service.api.repository.ModelResponse;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.util.XmlUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
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

    @Autowired
    private ObjectMapper objectMapper;


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
        pdConfig.setDeploymentId(processDefinition.getDeploymentId());
        pdConfig.setSuspended(processDefinition.isSuspended());
        pdConfig.setRejectGatewayBefore(false);
        pdConfig.setRejectParentProcess(false);
        processDefinitionConfigRepository.save(pdConfig);

    }


    /**
     * 导入模型文件
     *
     * @author:  zhaoyao
     * @param:  fileName 文件名
     * @param:  inputStream 文件流
     * @return: ModelResponse
     */
    public ModelResponse parseModelData(String fileName, InputStream inputStream) {

        String name,description,key;
        ObjectNode modelNode,propertiesNode;

        //导入格式为JSON
        if(fileName.endsWith(".json")){
            try {
                modelNode =  (ObjectNode)this.objectMapper.readTree(inputStream);
                propertiesNode = (ObjectNode)modelNode.get("properties");
                key = propertiesNode.get("process_id")==null?null: propertiesNode.get("process_id").asText();
                name = propertiesNode.get("name")==null?null: propertiesNode.get("name").asText();
                description = propertiesNode.get("documentation")==null?null: propertiesNode.get("documentation").asText();
            } catch (IOException e) {  throw new RuntimeException(e);  }
        }
        //导入格式为XML
        else if(fileName.endsWith(".xml")||fileName.endsWith(".bpmn")){
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
                BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
                if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
                    throw new BadRequestException("文件中未找到流程信息：" + fileName);
                }
                if (bpmnModel.getLocationMap().size() == 0) {
                    BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                    bpmnLayout.execute();
                }
                //XML数据转JSON
                BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
                modelNode = bpmnJsonConverter.convertToJson(bpmnModel);
                propertiesNode = (ObjectNode)modelNode.get("properties");
                //name、key、description
                Process process = bpmnModel.getMainProcess();
                key = process.getId();
                name = StringUtils.isNotEmpty(process.getName())?process.getName():process.getId();
                description = process.getDocumentation();
            } catch (XMLStreamException e) {throw new RuntimeException(e);}
        }else{
            throw new WorkflowException("不支持的文件类型！");
        }

        Model model =  repositoryService.newModel() ;
        model.setName(name);
        repositoryService.saveModel(model);
        //Key已经存在或者为空时重新生成,保证唯一性
        if(StringUtils.isNotBlank(key)){
            Model existModel = repositoryService.createModelQuery().modelKey(key).singleResult();
            if(existModel!=null) key += "_"+ model.getId();
        } else if(StringUtils.isBlank(key)){ //Key为空时生成新值
            key =  "M"+ model.getId();
        }
        model.setKey(key);
        modelNode.put("modelId",model.getId());
        propertiesNode.put("process_id",key);
        propertiesNode.put("process_author","zhaosan"); //TODO 创建人应该是当前登录用户

        //设置MetaInfo信息
        ObjectNode metaInfoNode = new ObjectMapper().createObjectNode();
        metaInfoNode.put("id", model.getId());
        metaInfoNode.put("process_id", key);
        ObjectNode stencilSetNode = this.objectMapper.createObjectNode();
        stencilSetNode.put("namespace", "http://b3mn.org/stencilset/bpmn2.0#");
        metaInfoNode.set("stencilset", stencilSetNode);
        //MetaInfo的properties
        ObjectNode propNode = this.objectMapper.createObjectNode();
        propNode.put("name", name);
        if(StringUtils.isNotBlank(description)) propNode.put("description", description);
        propNode.put("model_type", "model");
        //TODO 创建人、单位、部门 应该是当前登录用户所在的信息
        propNode.put("process_author", "zhaosan");
        propNode.put("department_id", "2000012");
        propNode.put("unit_id", "2000000");
        metaInfoNode.set("properties", propNode);
        model.setMetaInfo(metaInfoNode.toString());

        repositoryService.saveModel(model);
        repositoryService.addModelEditorSource(model.getId(),modelNode.toString().getBytes());

        return EntityModelUtil.toModelResponse(model);
    }



}
