package com.hy.workflow.service;

import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.ProcessDefinitionConfigModel;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import com.hy.workflow.util.EntityModelUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Date;
import java.util.Optional;

@Service
@Transactional
public class ProcessDefinitionService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;


    /**
     * 删除流程部署
     *
     * @author  zhaoyao
     * @param  deploymentId 部署ID
     * @param cascade 是否级联删除，
     * @return
     * @description cascade为false时：不级联删除，只能删除没有启动的流程，如果流程启动，会抛出异常
     *  cascade为true时：级联删除，删除和当前规则相关的所有信息，包括正在执行的流程和历史流程
     */
    public void deleteDeployment(String deploymentId, Boolean cascade) {
        ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deploymentId).singleResult();
        if(processDefinition!=null){
            repositoryService.deleteDeployment(deploymentId,cascade);
            processDefinitionConfigRepository.deleteByProcessDefinitionId(processDefinition.getId());
            flowElementConfigRepository.deleteByProcessDefinitionId(processDefinition.getId());
        }
    }


    /**
     * 删除多个流程部署`
     *
     * @author  zhaoyao
     * @param  deploymentIds 部署ID集合
     * @param cascade 是否级联删除，
     * @return
     */
    public void deleteDeployments(String[] deploymentIds, Boolean cascade) {
        for(String deploymentId : deploymentIds){
            this.deleteDeployment(deploymentId,cascade);
        }
    }


    /**
     * 获取流程配置
     *
     * @author  zhaoyao
     * @param  processDefinitionId 流程定义ID
     * @return ProcessDefinitionConfigModel 流程配置包装对象
     */
    public ProcessDefinitionConfigModel getProcessConfig(String processDefinitionId) {
        ProcessDefinitionConfig pdConfig = processDefinitionConfigRepository.findByProcessDefinitionId(processDefinitionId);
        return EntityModelUtil.toProcessDefinitionConfigModel(pdConfig);
    }


    /**
     * 保存流程配置
     *
     * @author  zhaoyao
     * @param  pdConfigModel 流程配置封装对象
     * @return ProcessDefinitionConfigModel 流程配置包装对象
     */
    public ProcessDefinitionConfigModel saveProcessConfig(ProcessDefinitionConfigModel pdConfigModel) {
        if(pdConfigModel==null||StringUtils.isBlank(pdConfigModel.getProcessDefinitionId())) throw new WorkflowException("流程定义ID不能为空！");
        Optional<ProcessDefinitionConfig>  sourceConfigOptional = processDefinitionConfigRepository.findById(pdConfigModel.getProcessDefinitionId());
        //新增
        if(!sourceConfigOptional.isPresent()){
            ProcessDefinitionConfig pdConfig = processDefinitionConfigRepository.save(new ProcessDefinitionConfig(pdConfigModel));
            return  EntityModelUtil.toProcessDefinitionConfigModel(pdConfig);
        }
        //修改
        else{
            ProcessDefinitionConfig sourceConfig = sourceConfigOptional.get();
            sourceConfig.setBusinessType(pdConfigModel.getBusinessType());
            sourceConfig.setUnitId(pdConfigModel.getUnitId());
            sourceConfig.setDepartmentId(pdConfigModel.getDepartmentId());
            sourceConfig.setDefaultProcess(pdConfigModel.getDefaultProcess());
            sourceConfig.setCallable(pdConfigModel.getCallable());
            sourceConfig.setDescription(pdConfigModel.getDescription());
            sourceConfig.setUpdateTime(new Date());
            processDefinitionConfigRepository.save(sourceConfig);
            return  EntityModelUtil.toProcessDefinitionConfigModel(sourceConfig);
        }
    }


    /**
     * 保存任务节点配置
     *
     * @author  zhaoyao
     * @param  model 任务节点配置封装对象
     * @return FlowElementConfigModel 任务节点配置包装对象
     */
    public FlowElementConfigModel saveElementConfig(FlowElementConfigModel model) {
        if(model==null||StringUtils.isBlank(model.getProcessDefinitionId())||StringUtils.isBlank(model.getFlowElementId()))
            throw new WorkflowException("流程定义ID和任务节点ID不能为空！");
        FlowElementConfig flowElementConfig = flowElementConfigRepository.findByProcessDefinitionIdAndFlowElementId(model.getProcessDefinitionId(),model.getFlowElementId());
        //新增
        if(flowElementConfig==null){
            model.setCreateTime(new Date());
            FlowElementConfig feConfig = flowElementConfigRepository.save(new FlowElementConfig(model));
            return  EntityModelUtil.toFlowElementConfigMode(feConfig);
        }
        //修改
        else{
            flowElementConfig.setMultiInstanceType(model.getMultiInstanceType());
            flowElementConfig.setAssigneeSelectOption(model.getAssigneeSelectOption());
            flowElementConfig.setAssigneeSelectScope(model.getAssigneeSelectScope());
            flowElementConfig.setCandidateGroups(model.getCandidateGroups());
            flowElementConfig.setCandidateUsers(model.getCandidateUsers());
            flowElementConfig.setCandidateJob(model.getCandidateJob());
            flowElementConfig.setCandidateRoles(model.getCandidateRoles());
            flowElementConfig.setFormField(model.getFormField());
            flowElementConfig.setTip(model.getTip());
            flowElementConfig.setEditForm(model.getEditForm());
            flowElementConfig.setAttachEditable(model.getAttachEditable());
            flowElementConfig.setRequireOpinion(model.getRequireOpinion());
            flowElementConfig.setShowApproveRecord(model.getShowApproveRecord());
            flowElementConfig.setRejectable(model.getRejectable());
            flowElementConfig.setSendCopy(model.getSendCopy());
            flowElementConfig.setUpdateTime(new Date());
            flowElementConfigRepository.save(flowElementConfig);
            return EntityModelUtil.toFlowElementConfigMode(flowElementConfig);
        }
    }


    /**
     * 获取任务节点配置
     *
     * @author  zhaoyao
     * @param  processDefinitionId 流程定义ID
     * @param  flowElementId 任务节点ID
     * @return FlowElementConfigModel 任务节点配置包装对象
     */
    public FlowElementConfigModel getFlowElementConfig(String processDefinitionId,String flowElementId) {
        FlowElementConfig feConfig = flowElementConfigRepository.findByProcessDefinitionIdAndFlowElementId(processDefinitionId,flowElementId);
        return EntityModelUtil.toFlowElementConfigMode(feConfig);
    }




}
