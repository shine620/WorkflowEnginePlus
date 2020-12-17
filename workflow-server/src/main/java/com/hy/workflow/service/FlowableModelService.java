package com.hy.workflow.service;

import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.FlowableModel;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ModelRequest;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.repository.FlowableModelRepository;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.DateUtils;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.ui.common.util.XmlUtil;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Flowable流程模型Service
 * @author zhoayao
 * @version 1.0
 *
 */
@Service
@Transactional
public class FlowableModelService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private FlowableModelRepository flowableModelRepository;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;


    /**
     * 通过ID查询模型
     *
     * @author:  zhaoyao
     * @param:  modelId  模型ID
     * @return: FlowableModel
     */
    public FlowableModel findById(String modelId) {
        Optional<FlowableModel> optional = flowableModelRepository.findById(modelId);
        return optional.isPresent()?optional.get():null;
    }


    /**
     * 保存模型
     *
     * @author:  zhaoyao
     * @param:  modelId  模型ID
     * @return: FlowableModel
     */
    public FlowableModel saveModel(ModelRequest modelRequest) {

        FlowableModel flowableModel;
        //新建模型
        if(StringUtils.isBlank(modelRequest.getId())){
            flowableModel = new FlowableModel();
            //TODO 设置创建人
            flowableModel.setCreateUser("zhangsan");
        }
        //修改模型
        else{
            Optional<FlowableModel> optional = flowableModelRepository.findById(modelRequest.getId());
            flowableModel = optional.isPresent()?optional.get():new FlowableModel();
        }

        try {
            ByteArrayInputStream byteInputStream = new ByteArrayInputStream(modelRequest.getXml().getBytes());
            XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
            InputStreamReader xmlIn = new InputStreamReader(byteInputStream, StandardCharsets.UTF_8);
            XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
            BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
            BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
            //验证模型
            WorkflowUtil.validateBpmnModel(bpmnModel);
            //设置布局
            if (bpmnModel.getLocationMap().size() == 0) {
                BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                bpmnLayout.execute();
            }

            Process process = bpmnModel.getMainProcess();
            String key = process.getId();
            String name = StringUtils.isNotEmpty(process.getName())?process.getName():process.getId();
            String description = process.getDocumentation();

            //模型Key已经存在时重新生成,保证唯一性
            FlowableModel model = flowableModelRepository.findByModelKey(key);
            if(model!=null){
                key += DateUtils.formatDate(new Date(),"yyyyMMddHHmmssSSS")+ RandomStringUtils.randomAlphanumeric(5);
                process.setId(key);
            }
            //XML数据
            byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
            //生成模型信息
            flowableModel.setModelKey(key);
            flowableModel.setName(name);
            flowableModel.setDescription(description);
            flowableModel.setXml(new String(bpmnBytes));
            flowableModel.setSvg(modelRequest.getSvg());
            //TODO 设置修改人
            flowableModel.setLastUpdatedUser("lisi");
            flowableModelRepository.save(flowableModel);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        return flowableModel;
    }


    /**
     * 删除模型
     *
     * @author:  zhaoyao
     * @param:  modelId  模型ID
     */
    public void deleteModel(String modelId) {
        flowableModelRepository.deleteById(modelId);
    }


    /**
     * 批量删除模型
     *
     * @author:  zhaoyao
     * @param:  modelId  模型ID数组
     */
    public void batchDeleteModel(String[] modelIds) {
        flowableModelRepository.deleteByIdIn(modelIds);
    }


    /**
     * 部署模型
     *
     * @author:  zhaoyao
     * @param:  model 模型实体
     * @return: Model
     */
    public void deploy(FlowableModel model) {

        String processName = model.getName() + ".bpmn20.xml";
        Deployment deployment = repositoryService.createDeployment().name(model.getName()).addString(processName, model.getXml()).deploy();

        //解析XML生成模型配置
        ByteArrayInputStream byteInputStream = new ByteArrayInputStream(model.getXml().getBytes());
        XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
        InputStreamReader xmlIn = new InputStreamReader(byteInputStream, StandardCharsets.UTF_8);
        XMLStreamReader xtr = null;
        try { xtr = xif.createXMLStreamReader(xmlIn);
        } catch (XMLStreamException e) {
            throw new RuntimeException(e);
        }
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
        Process process = bpmnModel.getMainProcess();

        //生成流程定义配置信息
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();
        parseProcessDefinitionConfig(processDefinition,process); //定义配置
        parseFlowElementConfig(processDefinition.getId(),process.getFlowElements()); //节点配置
    }


    //XML文件中解析流程定义配置
    private void parseProcessDefinitionConfig(ProcessDefinition processDefinition,Process process){
        ProcessDefinitionConfig pdConfig = new ProcessDefinitionConfig();
        Map<String, List<ExtensionElement>> extensions = process.getExtensionElements();
        if(extensions.containsKey("businessType")){
            List<ExtensionElement> list = extensions.get("businessType");
            pdConfig.setBusinessType(list.get(0).getElementText());
        }
        if(extensions.containsKey("unitId")){
            List<ExtensionElement> list = extensions.get("unitId");
            pdConfig.setUnitId(list.get(0).getElementText());
        }
        if(extensions.containsKey("departmentId")){
            List<ExtensionElement> list = extensions.get("departmentId");
            pdConfig.setDepartmentId(list.get(0).getElementText());
        }
        if(extensions.containsKey("callable")){
            List<ExtensionElement> list = extensions.get("callable");
            pdConfig.setCallable(Boolean.valueOf(list.get(0).getElementText()));
        }
        if(extensions.containsKey("defaultProcess")){
            List<ExtensionElement> list = extensions.get("defaultProcess");
            pdConfig.setDefaultProcess(Boolean.valueOf(list.get(0).getElementText()));
        }
        if(extensions.containsKey("rejectParentProcess")){
            List<ExtensionElement> list = extensions.get("rejectParentProcess");
            pdConfig.setRejectParentProcess(Boolean.valueOf(list.get(0).getElementText()));
        }
        if(extensions.containsKey("rejectGatewayBefore")){
            List<ExtensionElement> list = extensions.get("rejectGatewayBefore");
            pdConfig.setRejectGatewayBefore(Boolean.valueOf(list.get(0).getElementText()));
        }
        pdConfig.setProcessDefinitionId(processDefinition.getId());
        pdConfig.setProcessDefinitionKey(processDefinition.getKey());
        pdConfig.setProcessDefinitionName(processDefinition.getName());
        pdConfig.setVersion(processDefinition.getVersion());
        pdConfig.setDeploymentId(processDefinition.getDeploymentId());
        pdConfig.setSuspended(processDefinition.isSuspended());
        pdConfig.setDescription(processDefinition.getDescription());
        //TODO  创建修改人应为当前登录用户
        pdConfig.setCreateUser("LiBai");
        pdConfig.setUpdateUser("Dufu");
        processDefinitionConfigRepository.save(pdConfig);
    }


    //XML文件中解析用户任务节点配置
    private void parseFlowElementConfig(String processDefinitionId,Collection<FlowElement> flowElements){
        for(FlowElement e : flowElements) {
            FlowElementConfig elementConfig = new FlowElementConfig();
            elementConfig.setFlowElementId(e.getId());
            elementConfig.setProcessDefinitionId(processDefinitionId);
            if(e instanceof UserTask){
                Map<String, List<ExtensionElement>> extensionElements = e.getExtensionElements();
                if(extensionElements.containsKey("multiUser")){
                    elementConfig.setMultiUser(Boolean.valueOf(extensionElements.get("multiUser").get(0).getElementText()));
                }
                if(extensionElements.containsKey("fixed")){
                    elementConfig.setFixed(Boolean.valueOf(extensionElements.get("fixed").get(0).getElementText()));
                }
                if(extensionElements.containsKey("assigneeOption")){
                    elementConfig.setAssigneeOption(extensionElements.get("assigneeOption").get(0).getElementText());
                }
                if(extensionElements.containsKey("orgScope")){
                    elementConfig.setOrgScope(extensionElements.get("orgScope").get(0).getElementText());
                }
                if(extensionElements.containsKey("orgValue")){
                    elementConfig.setOrgValue(extensionElements.get("orgValue").get(0).getElementText());
                }
                if(extensionElements.containsKey("userValue")){
                    elementConfig.setUserValue(extensionElements.get("userValue").get(0).getElementText());
                }
                if(extensionElements.containsKey("positionValue")){
                    elementConfig.setPositionValue(extensionElements.get("positionValue").get(0).getElementText());
                }
                if(extensionElements.containsKey("roleValue")){
                    elementConfig.setRoleValue(extensionElements.get("roleValue").get(0).getElementText());
                }
                if(extensionElements.containsKey("autoSelect")){
                    elementConfig.setAutoSelect(Boolean.valueOf(extensionElements.get("autoSelect").get(0).getElementText()));
                }
                if(extensionElements.containsKey("editForm")){
                    elementConfig.setEditForm(Boolean.valueOf(extensionElements.get("editForm").get(0).getElementText()));
                }
                if(extensionElements.containsKey("requireOpinion")){
                    elementConfig.setRequireOpinion(Boolean.valueOf(extensionElements.get("requireOpinion").get(0).getElementText()));
                }
                if(extensionElements.containsKey("showApproveRecord")){
                    elementConfig.setShowApproveRecord(Boolean.valueOf(extensionElements.get("showApproveRecord").get(0).getElementText()));
                }
                if(extensionElements.containsKey("rejectable")){
                    elementConfig.setRejectable(Boolean.valueOf(extensionElements.get("rejectable").get(0).getElementText()));
                }
                if(extensionElements.containsKey("sendCopy")){
                    elementConfig.setSendCopy(Boolean.valueOf(extensionElements.get("sendCopy").get(0).getElementText()));
                }
                elementConfig.setFlowElementType(FlowElementType.USER_TASK);
                flowElementConfigRepository.save(elementConfig);
            }else if(e instanceof SubProcess){
                elementConfig.setFlowElementType(FlowElementType.SUB_PROCESS);
                Collection<FlowElement> subNodes =((SubProcess) e).getFlowElements();
                parseFlowElementConfig(processDefinitionId,subNodes);
            }/*else if(e instanceof CallActivity){
                elementConfig.setFlowElementType(FlowElementType.CALL_ACTIVITY);
            }
            flowElementConfigRepository.save(elementConfig);*/
        }
    }


    /**
     * 导入模型文件
     *
     * @author:  zhaoyao
     * @param:  fileName 文件名
     * @param:  inputStream 文件流
     * @return: Model
     */
    public FlowableModel importModel(String fileName, InputStream inputStream) {

        //导入格式为XML
        if(fileName.endsWith(".xml")||fileName.endsWith(".bpmn")){
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
                BpmnModel bpmnModel = bpmnXMLConverter.convertToBpmnModel(xtr);
                WorkflowUtil.validateBpmnModel(bpmnModel);
                //设置布局
                if (bpmnModel.getLocationMap().size() == 0) {
                    BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                    bpmnLayout.execute();
                }

                Process process = bpmnModel.getMainProcess();
                String key = process.getId();
                String name = StringUtils.isNotEmpty(process.getName())?process.getName():process.getId();
                String description = process.getDocumentation();

                //模型Key已经存在时重新生成,保证唯一性
                FlowableModel model = flowableModelRepository.findByModelKey(key);
                if(model!=null){
                    key += DateUtils.formatDate(new Date(),"yyyyMMddHHmmssSSS")+ RandomStringUtils.randomAlphanumeric(5);
                    process.setId(key);
                }
                //XML数据
                byte[] bpmnBytes = new BpmnXMLConverter().convertToXML(bpmnModel);
                //生成模型信息
                FlowableModel flowableModel = new FlowableModel();
                flowableModel.setModelKey(key);
                flowableModel.setName(name);
                flowableModel.setDescription(description);
                flowableModel.setXml(new String(bpmnBytes));
                //TODO 设置创建人和修改人
                flowableModel.setCreateUser("zhangsan");
                flowableModel.setLastUpdatedUser("lisi");
                flowableModelRepository.save(flowableModel);
                //部署测试
                //String processName = name+ ".bpmn20.xml";
                //Deployment deployment = repositoryService.createDeployment().name(name).addBytes(processName,bpmnBytes).deploy();
                //repositoryService.createDeployment().name(model.getName()).addString(processName, new String(bpmnBytes)).deploy();
                return flowableModel;
            } catch (XMLStreamException e) {throw new RuntimeException(e);}

        }else{
            throw new WorkflowException("不支持的文件类型！");
        }
    }


    /**
     * 获取流程模型列表
     *
     * @author  zhaoyao
     * @param  modelRequest 组合条件参数封装
     * @param  pageRequest 分页参数
     * @return PageBean<FlowableModel> 流程模型分页数据
     */
    public PageBean<FlowableModel> findModelList(ModelRequest modelRequest, PageRequest pageRequest) {
        Page<FlowableModel> models = findByConditions(modelRequest,pageRequest);
        PageBean page = new PageBean(models);
        return page;
    }


    //动态查询方法(分页)
    private Page<FlowableModel> findByConditions(ModelRequest modelRequest, PageRequest pageRequest ) {
        Specification<FlowableModel> specification = (Specification<FlowableModel>) (root, criteriaQuery, criteriaBuilder) -> {
            //设置查询条件
            Predicate[] predicates= generatePredicates(modelRequest,root,criteriaBuilder);
            Predicate predicate = criteriaBuilder.and( predicates );
            return predicate;
        };
        return flowableModelRepository.findAll(specification, pageRequest);
    }


    //动态查询条件
    private Predicate[] generatePredicates(ModelRequest modelRequest, Root<?> root, CriteriaBuilder criteriaBuilder){
        List<Predicate> predicatesList = new ArrayList<>();
        if (StringUtils.isNotBlank(modelRequest.getName())) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("name"), "%" + modelRequest.getName() + "%"));
            predicatesList.add(predicate);
        }
        if (StringUtils.isNotBlank(modelRequest.getId())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("id"),  modelRequest.getId()) )  );
        }
        if (StringUtils.isNotBlank(modelRequest.getModelKey())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("modelKey"),  modelRequest.getModelKey()) )  );
        }
        if (StringUtils.isNotBlank(modelRequest.getCreateUser())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("createUser"),  modelRequest.getCreateUser()) )  );
        }
        return predicatesList.toArray(new Predicate[predicatesList.size()]);
    }



}
