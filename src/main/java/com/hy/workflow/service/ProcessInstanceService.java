package com.hy.workflow.service;

import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.model.ProcessInstanceModel;
import com.hy.workflow.model.StartProcessRequest;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.MultiInstanceRecordRepository;
import com.hy.workflow.repository.RejectRecordRepository;
import com.hy.workflow.repository.TaskRecordRepository;
import com.hy.workflow.util.EntityModelUtil;
import com.hy.workflow.util.ValidateUtil;
import com.hy.workflow.util.WorkflowUtil;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import java.util.*;

@Service
@Transactional
public class ProcessInstanceService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Autowired
    private RejectRecordRepository rejectRecordRepository;

    @Autowired
    private MultiInstanceRecordRepository multiInstanceRecordRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Resource
    private EntityManager entityManager;


    /**
     * 发起流程
     *
     * @author  zhaoyao
     * @param  processDefinition 流程定义对象
     * @param  startRequest 发起流程请求参数
     * @return ProcessInstanceModel 流程实例封装对象
     */
    public ProcessInstanceModel startProcess(ProcessDefinition processDefinition, StartProcessRequest startRequest) {

        Authentication.setAuthenticatedUserId(startRequest.getStartUserId());
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())
                .businessKey(startRequest.getBusinessType()+";"+startRequest.getBusinessId())
                .name( startRequest.getBusinessName()==null?"":startRequest.getBusinessName()  +"-" +processDefinition.getName() )
                .variables(startRequest.getVariables());

        ProcessInstance instance = processInstanceBuilder.start();
        /*这个方法最终使用一个ThreadLocal类型的变量进行存储，也就是与当前的线程绑定，
        所以流程实例启动完毕之后，需要设置为null，防止多线程的时候出问题*/
        Authentication.setAuthenticatedUserId(null);

        //设置下一环节处理信息
        Map<String,Object> variables = new HashMap();
        WorkflowUtil.setNextTaskInfoVariables(variables,startRequest);

        //所选择的流程分支节点
        List<String> selectOutNode = new ArrayList<>();
        if(startRequest.getNextTaskList()!=null){
            startRequest.getNextTaskList().forEach(nextTask ->{
                selectOutNode.add(nextTask.getGroupId());
            });
        }

        //获取第一个节点
        BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());
        Process process =  model.getMainProcess();
        FlowElement startNode = process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) ((StartEvent) startNode).getOutgoingFlows().get(0).getTargetFlowElement();

        Task firstTask = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        taskService.setAssignee(firstTask.getId(),startRequest.getStartUserId());

        //第一个节点要自动审批(承办人发起环节)
        WorkflowUtil.completeTaskBySelectNode(selectOutNode,firstNode,taskService,firstTask,variables);

        //保存业务实例数据，一个流程实例对应一个业务实例
        BusinessProcess bp = new BusinessProcess();
        EntityModelUtil.fillBusinessProcess(bp,instance);
        bp.setBusinessId(startRequest.getBusinessId());
        bp.setBusinessType(startRequest.getBusinessType());
        bp.setBusinessName(startRequest.getBusinessName());
        bp.setBusinessUrl(startRequest.getBusinessUrl());
        bp.setUnitId(startRequest.getUnitId());
        bp.setDeptId(startRequest.getDeptId());
        businessProcessRepository.save(bp);
        ProcessInstanceModel instanceModel = EntityModelUtil.toProcessInstanceModel(bp);

        return instanceModel;
    }


    /**
     * 删除流程实例
     *
     * @author  zhaoyao
     * @param  processInstanceId 流程实例ID
     * @param  deleteReason 删除原因
     * @return
     */
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        //同步删除业务表数据信息
        businessProcessRepository.deleteById(processInstanceId);
        List<HistoricProcessInstance> subProcessList = historyService.createHistoricProcessInstanceQuery().superProcessInstanceId(processInstanceId).list();
        List<String> processInstanceIds = new ArrayList<>();
        processInstanceIds.add(processInstanceId);
        subProcessList.forEach(historicProcessInstance -> {
            processInstanceIds.add(historicProcessInstance.getId());
        });
        taskRecordRepository.deleteByProcessInstanceIdIn(processInstanceIds);
        multiInstanceRecordRepository.deleteByProcessInstanceIdIn(processInstanceIds);
        rejectRecordRepository.deleteByProcessInstanceIdIn(processInstanceIds);
        businessProcessRepository.deleteByProcessInstanceIdIn(processInstanceIds);
        //删除流程表数据
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count();
        long historyCount = 0;
        if(count>0){
            runtimeService.deleteProcessInstance(processInstanceId,deleteReason);
            historyService.deleteHistoricProcessInstance(processInstanceId);
        }else{
            historyCount = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count();
            if(historyCount>0) historyService.deleteHistoricProcessInstance(processInstanceId);
        }
        if( count==0 && historyCount==0 ) throw new WorkflowException("流程实例删除失败，该实例不存在："+processInstanceId);

    }


    /**
     * 获取流程实例数据
     *
     * @author  zhaoyao
     * @param  processInstanceId 流程实例ID
     * @return ProcessInstanceModel 流程实例封装对象
     */
    public ProcessInstanceModel getProcessInstance(String processInstanceId) {
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count();
        if(count ==0){
            long historyCount = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count();
            if(historyCount == 0) return null;
        }
        Optional<BusinessProcess> op = businessProcessRepository.findById(processInstanceId);
        BusinessProcess bp = op.isPresent()?op.get():null;
        return EntityModelUtil.toProcessInstanceModel(bp);
    }


    /**
     * 挂起/激活流程实例
     *
     * @author  zhaoyao
     * @param  processInstanceId 流程实例ID
     * @return void
     */
    public void suspendProcessInstance(String processInstanceId, Boolean suspend) {
        Optional<BusinessProcess> op = businessProcessRepository.findById(processInstanceId);
        BusinessProcess bp = op.isPresent()?op.get():null;
        if(bp==null) throw new WorkflowException("挂起或激活失败，该流程实例不存在："+processInstanceId);
        if(suspend){
            runtimeService.suspendProcessInstanceById(processInstanceId);
            bp.setSuspended(true);
        }else{
            runtimeService.activateProcessInstanceById(processInstanceId);
            bp.setSuspended(false);
        }
        businessProcessRepository.save(bp);
    }


    /**
     * 获取流程实例数据
     *
     * @author  zhaoyao
     * @param  model 流程实例请求参数对象
     * @return List<ProcessInstanceModel> 流程实例数据的集合
     */
    public List<ProcessInstanceModel> findInstanceList(ProcessInstanceModel model) {
        List<ProcessInstanceModel> instances = new ArrayList<>();
        List<BusinessProcess> psList = findByConditions(model);
        psList.forEach(bp->{
            instances.add(EntityModelUtil.toProcessInstanceModel(bp));
        });
        return instances;
    }


    /**
     * 获取流程实例数据(分页)
     *
     * @author  zhaoyao
     * @param  model 流程实例请求参数对象
     * @return PageBean<ProcessInstanceModel> 流程实例分页数据
     */
    public PageBean<ProcessInstanceModel> findInstanceList(ProcessInstanceModel model, PageRequest pageRequest) {
        ValidateUtil.checkPageNum(pageRequest);
        List<ProcessInstanceModel> instances = new ArrayList<>();
        Page<BusinessProcess> pageInstances = findByConditions(model,pageRequest);
        pageInstances.forEach(bp->{
            instances.add(EntityModelUtil.toProcessInstanceModel(bp));
        });
        PageBean page = new PageBean(pageInstances);
        page.setData(instances);
        return page;
    }


    //动态查询方法
    private List<BusinessProcess> findByConditions(ProcessInstanceModel model) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<BusinessProcess> query = criteriaBuilder.createQuery(BusinessProcess.class);
        //Root 定义查询的From子句中能出现的类型
        Root<BusinessProcess> root = query.from(BusinessProcess.class);
        //Predicate 拼接查询条件
        Predicate[] predicatesList = generatePredicates(model,root,criteriaBuilder);
        query.where(predicatesList);
        //发起时间和实例ID排序
        query.orderBy( criteriaBuilder.desc(root.get("startTime")), criteriaBuilder.desc(root.get("processInstanceId")) );
        TypedQuery<BusinessProcess> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }


    //动态查询方法(分页)
    private Page<BusinessProcess> findByConditions(ProcessInstanceModel model, PageRequest pageRequest ) {
        Specification<BusinessProcess> specification = (Specification<BusinessProcess>) (root, criteriaQuery, criteriaBuilder) -> {
            //设置查询条件
            Predicate[] predicates= generatePredicates(model,root,criteriaBuilder);
            //设置排序方式(PageRequest对象中指定了排序方式后，此处无效)
            //criteriaQuery.orderBy( criteriaBuilder.desc(root.get("startTime")), criteriaBuilder.desc(root.get("processInstanceId")) );
            Predicate predicate = criteriaBuilder.and( predicates );
            return predicate;
        };
        return businessProcessRepository.findAll(specification, pageRequest);
    }


    //动态查询条件
    private Predicate[] generatePredicates(ProcessInstanceModel model,Root<BusinessProcess> root, CriteriaBuilder criteriaBuilder){
        List<Predicate> predicatesList = new ArrayList<>();
        if (model.getProcessInstanceName()  != null) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("processInstanceName"), "%" + model.getProcessInstanceName() + "%"));
            predicatesList.add(predicate);
        }
        if (model.getProcessDefinitionName()  != null) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("processDefinitionName"), "%" + model.getProcessDefinitionName() + "%"));
            predicatesList.add(predicate);
        }
        if (model.getBusinessName()  != null) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("businessName"), "%" + model.getBusinessName() + "%"));
            predicatesList.add(predicate);
        }
        if (model.getDeploymentId()  != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("deploymentId"),  model.getDeploymentId()) )  );
        }
        if (model.getBusinessId() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("businessId"),  model.getBusinessId()) )  );
        }
        if (model.getProcessInstanceId() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("processInstanceId"),  model.getProcessInstanceId()) )  );
        }
        if (model.getStartTime() != null) {
            Predicate startTime =  criteriaBuilder.greaterThanOrEqualTo(root.get("startTime").as(Date.class), model.getStartTime());
            predicatesList.add(startTime);
        }
        if (model.getEndTime() != null) {
            Predicate endTime =  criteriaBuilder.lessThanOrEqualTo(root.get("endTime").as(Date.class), model.getEndTime());
            predicatesList.add(endTime);
        }
        if (model.getSuspended() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("suspended"),  model.getSuspended()) )  );
        }
        if (model.getEnded() != null) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("ended"),  model.getEnded()) )  );
        }
        return predicatesList.toArray(new Predicate[predicatesList.size()]);
    }




}
