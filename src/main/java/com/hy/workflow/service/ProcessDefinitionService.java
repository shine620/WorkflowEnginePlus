package com.hy.workflow.service;

import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.*;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.ProcessDefinitionConfigModel;
import com.hy.workflow.repository.*;
import com.hy.workflow.util.EntityModelUtil;
import com.hy.workflow.util.ValidateUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.*;
import java.util.*;

@Service
@Transactional
public class ProcessDefinitionService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Autowired
    private MultiInstanceRecordRepository multiInstanceRecordRepository;

    @PersistenceContext
    private EntityManager entityManager;

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
            if(cascade) {
                //查询该流程定义产生的流程实例
                /*List<HistoricProcessInstance> instanceList = historyService.createHistoricProcessInstanceQuery().processDefinitionId(processDefinition.getId()).list();
                List<String> instanceIdList = new ArrayList<>();
                instanceList.forEach(historicProcessInstance -> {
                    instanceIdList.add(historicProcessInstance.getId());
                });
                //删除对应的BusinessProcess数据
                List<BusinessProcess> bpList = businessProcessRepository.findAllById(instanceIdList);
                businessProcessRepository.deleteInBatch(bpList);*/
                businessProcessRepository.deleteByProcessDefinitionId(processDefinition.getId());
                taskRecordRepository.deleteByProcessDefinitionId(processDefinition.getId());
                multiInstanceRecordRepository.deleteByProcessDefinitionId(processDefinition.getId());
            }
            repositoryService.deleteDeployment(deploymentId,cascade);
            processDefinitionConfigRepository.deleteByProcessDefinitionId(processDefinition.getId());
            flowElementConfigRepository.deleteByProcessDefinitionId(processDefinition.getId());
        }
    }


    /**
     * 删除多个流程部署
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
     * 挂起流程
     *
     * @author  zhaoyao
     * @param  processDefinitionId 流程定义
     * @param  suspend 是否挂起
     */
    public void suspendProcessDefinitionById(String processDefinitionId, Boolean suspend) {
        //ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(processDefinitionId).singleResult();
        if(suspend){
            repositoryService.suspendProcessDefinitionById(processDefinitionId);
        }else{
            repositoryService.activateProcessDefinitionById(processDefinitionId);
        }
        Optional<ProcessDefinitionConfig>  sourceConfigOptional = processDefinitionConfigRepository.findById(processDefinitionId);
        if(!sourceConfigOptional.isPresent()){
            ProcessDefinitionConfig sourceConfig = sourceConfigOptional.get();
            sourceConfig.setSuspended(suspend);
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


    /**
     * 查询流程定义配置列表
     *
     * @author  zhaoyao
     * @param  model 组合条件参数封装
     * @return List<ProcessDefinitionConfigModel> 流程定义配置数据
     */
    public List<ProcessDefinitionConfigModel> findProcessDefinitionConfigList(ProcessDefinitionConfigModel model ) {
        Specification<ProcessDefinitionConfig> specification = (Specification<ProcessDefinitionConfig>) (root, criteriaQuery, criteriaBuilder) -> {
            //设置查询条件
            Predicate[] predicates= generatePredicates(model,root,criteriaBuilder);
            Predicate predicate = criteriaBuilder.and( predicates );
            return predicate;
        };
        List<ProcessDefinitionConfig> configs = processDefinitionConfigRepository.findAll(specification);
        List<ProcessDefinitionConfigModel> configModelList = new ArrayList<>();
        configs.forEach(bp->{
            configModelList.add(EntityModelUtil.toProcessDefinitionConfigModel(bp));
        });
        return configModelList;
    }


    /**
     * 查询最新的流程定义配置列表
     *
     * @author  zhaoyao
     * @param  model 组合条件参数封装
     * @return List<ProcessDefinitionConfigModel> 流程定义配置数据
     */
    public List<ProcessDefinitionConfigModel> findProcessDefinitionConfigLaterstList(ProcessDefinitionConfigModel model ) {

        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<ProcessDefinitionConfigModel> criteriaQuery = criteriaBuilder.createQuery(ProcessDefinitionConfigModel.class);
        Root<ProcessDefinitionConfig> root = criteriaQuery.from(ProcessDefinitionConfig.class);
        //注意这里的字段设置顺序要与ProcessDefinitionConfigModel构造方法一致
        criteriaQuery.multiselect(root.get("processDefinitionId"),root.get("processDefinitionKey"),
                root.get("processDefinitionName"),criteriaBuilder.max(root.get("version")).alias("version"),root.get("description"),root.get("suspended"),
                root.get("createUserId"),root.get("createDeptId"),root.get("createUnitId"),
                root.get("createTime"),root.get("updateTime"),root.get("businessType"),
                root.get("departmentId"),root.get("unitId"),root.get("deploymentId"),
                root.get("callable"),root.get("defaultProcess"),root.get("rejectParentProcess"),root.get("rejectGatewayBefore"));
        Predicate[] predicates= generatePredicates(model,root,criteriaBuilder);
        Predicate predicate = criteriaBuilder.and( predicates );
        Order createTimeOrder = criteriaBuilder.desc(root.get("createTime"));
        criteriaQuery.where(predicate).groupBy(root.get("processDefinitionKey")).orderBy(createTimeOrder);
        List<ProcessDefinitionConfigModel> configs = entityManager.createQuery(criteriaQuery).getResultList();
        return configs;
    }


    /**
     * 获取流程定义配置列表
     *
     * @author  zhaoyao
     * @param  model 组合条件参数封装
     * @param  pageRequest 分页参数
     * @return PageBean<ProcessDefinitionConfigModel> 流程定义配置分页数据
     */
    public PageBean<ProcessDefinitionConfigModel> findProcessDefinitionConfigList(ProcessDefinitionConfigModel model, PageRequest pageRequest) {
        ValidateUtil.checkPageNum(pageRequest);
        List<ProcessDefinitionConfigModel> configList = new ArrayList<>();
        Page<ProcessDefinitionConfig> definitionConfigs = findByConditions(model,pageRequest);
        definitionConfigs.forEach(bp->{
            configList.add(EntityModelUtil.toProcessDefinitionConfigModel(bp));
        });
        PageBean page = new PageBean(definitionConfigs);
        page.setData(configList);
        return page;
    }


    //动态查询方法(分页)
    private Page<ProcessDefinitionConfig> findByConditions(ProcessDefinitionConfigModel model, PageRequest pageRequest ) {
        Specification<ProcessDefinitionConfig> specification = (Specification<ProcessDefinitionConfig>) (root, criteriaQuery, criteriaBuilder) -> {
            //设置查询条件
            Predicate[] predicates= generatePredicates(model,root,criteriaBuilder);
            Predicate predicate = criteriaBuilder.and( predicates );
            return predicate;
        };
        return processDefinitionConfigRepository.findAll(specification, pageRequest);
    }


    //动态查询条件
    private Predicate[] generatePredicates(ProcessDefinitionConfigModel model, Root<ProcessDefinitionConfig> root, CriteriaBuilder criteriaBuilder){
        List<Predicate> predicatesList = new ArrayList<>();
        if (model.getProcessDefinitionName()  != null) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("processDefinitionName"), "%" + model.getProcessDefinitionName() + "%"));
            predicatesList.add(predicate);
        }
        if (model.getDeploymentId()  != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("deploymentId"),  model.getDeploymentId()) )  );
        }
        if (model.getBusinessType()  != null) {
            Predicate[] arr = new Predicate[4];
            arr[0] = criteriaBuilder.equal( root.get("businessType"),  model.getBusinessType()) ;
            arr[1]=  criteriaBuilder.like( root.get("businessType"), "%," + model.getBusinessType()+",%");
            arr[2] = criteriaBuilder.like( root.get("businessType"), model.getBusinessType()+",%");
            arr[3] = criteriaBuilder.like( root.get("businessType"), "%," + model.getBusinessType());
            predicatesList.add( criteriaBuilder.or(arr) );
        }
        if (model.getUnitId()  != null) {
            Predicate[] arr = new Predicate[4];
            arr[0] = criteriaBuilder.equal( root.get("unitId"),  model.getUnitId()) ;
            arr[1]=  criteriaBuilder.like( root.get("unitId"), "%," + model.getUnitId()+",%");
            arr[2] = criteriaBuilder.like( root.get("unitId"), model.getUnitId()+",%");
            arr[3] = criteriaBuilder.like( root.get("unitId"), "%," + model.getUnitId());
            predicatesList.add( criteriaBuilder.or(arr) );
        }
        if (model.getDepartmentId()  != null) {
            Predicate[] arr = new Predicate[4];
            arr[0] = criteriaBuilder.equal( root.get("departmentId"),  model.getDepartmentId()) ;
            arr[1]=  criteriaBuilder.like( root.get("departmentId"), "%," + model.getDepartmentId()+",%");
            arr[2] = criteriaBuilder.like( root.get("departmentId"), model.getDepartmentId()+",%");
            arr[3] = criteriaBuilder.like( root.get("departmentId"), "%," + model.getDepartmentId());
            predicatesList.add( criteriaBuilder.or(arr) );
        }
        if (model.getCreateUnitId() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("createUnitId"),  model.getCreateUnitId()) )  );
        }
        if (model.getCreateTime() != null) {
            Predicate endTime =  criteriaBuilder.greaterThanOrEqualTo(root.get("createTime").as(Date.class), model.getCreateTime());
            predicatesList.add(endTime);
        }
        if (model.getSuspended() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("suspended"),  model.getSuspended()) )  );
        }

        return predicatesList.toArray(new Predicate[predicatesList.size()]);
    }




}
