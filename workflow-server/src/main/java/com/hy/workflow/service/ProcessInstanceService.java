package com.hy.workflow.service;

import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveInfo;
import com.hy.workflow.model.ProcessInstanceModel;
import com.hy.workflow.model.StartProcessRequest;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.MultiInstanceRecordRepository;
import com.hy.workflow.repository.RejectRecordRepository;
import com.hy.workflow.repository.TaskRecordRepository;
import com.hy.workflow.util.EntityModelUtil;
import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.lang3.StringUtils;
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
     * ????????????
     *
     * @author  zhaoyao
     * @param  processDefinition ??????????????????
     * @param  startRequest ????????????????????????
     * @return ProcessInstanceModel ????????????????????????
     */
    public ProcessInstanceModel startProcess(ProcessDefinition processDefinition, StartProcessRequest startRequest) {

        Authentication.setAuthenticatedUserId(startRequest.getUserId());
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())
                .businessKey(startRequest.getBusinessType()+";"+startRequest.getBusinessId())
                .name( StringUtils.isBlank(startRequest.getBusinessName())?processDefinition.getName():startRequest.getBusinessName())
                .variables(startRequest.getVariables());

        ProcessInstance instance = processInstanceBuilder.start();
        /*??????????????????????????????ThreadLocal??????????????????????????????????????????????????????????????????
        ??????????????????????????????????????????????????????null????????????????????????????????????*/
        Authentication.setAuthenticatedUserId(null);

        //??????????????????????????????
        Map<String,Object> variables = new HashMap();
        WorkflowUtil.setNextTaskInfoVariables(variables,startRequest);

        //??????????????????????????????
        List<String> selectOutNode = new ArrayList<>();
        if(startRequest.getNextTaskList()!=null){
            startRequest.getNextTaskList().forEach(nextTask ->{
                if(nextTask.getParentFlowElementId()!=null)
                    selectOutNode.add(nextTask.getParentFlowElementId());
                else
                    selectOutNode.add(nextTask.getFlowElementId());
            });
        }

        //?????????????????????
        BpmnModel model = repositoryService.getBpmnModel(processDefinition.getId());
        Process process =  model.getMainProcess();
        FlowElement startNode = process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) ((StartEvent) startNode).getOutgoingFlows().get(0).getTargetFlowElement();

        Task firstTask = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        taskService.setAssignee(firstTask.getId(),startRequest.getUserId());
        //??????????????????
        if(StringUtils.isNotBlank(startRequest.getOpinion()))taskService.addComment(firstTask.getId(), instance.getId(), startRequest.getOpinion());

        //??????????????????????????????(?????????????????????)
        WorkflowUtil.completeTaskBySelectNode(selectOutNode,firstNode,taskService,firstTask,variables);

        //?????????????????????????????????????????????????????????????????????
        BusinessProcess bp = new BusinessProcess();
        EntityModelUtil.fillBusinessProcess(bp,instance);
        bp.setBusinessId(startRequest.getBusinessId());
        bp.setBusinessType(startRequest.getBusinessType());
        bp.setBusinessName(startRequest.getBusinessName());
        bp.setEditUrl(startRequest.getEditUrl());
        bp.setViewUrl(startRequest.getViewUrl());
        businessProcessRepository.save(bp);
        ProcessInstanceModel instanceModel = EntityModelUtil.toProcessInstanceModel(bp);
        return instanceModel;
    }


    /**
     * ??????????????????
     *
     * @author  zhaoyao
     * @param  processInstanceId ????????????ID
     * @param  deleteReason ????????????
     * @return
     */
    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        //?????????????????????????????????
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
        //?????????????????????
        long count = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).count();
        long historyCount = 0;
        if(count>0){
            runtimeService.deleteProcessInstance(processInstanceId,deleteReason);
            historyService.deleteHistoricProcessInstance(processInstanceId);
        }else{
            historyCount = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count();
            if(historyCount>0) historyService.deleteHistoricProcessInstance(processInstanceId);
        }
        if( count==0 && historyCount==0 ) throw new WorkflowException("????????????????????????????????????????????????"+processInstanceId);

    }


    /**
     * ????????????????????????
     *
     * @author  zhaoyao
     * @param  processInstanceId ????????????ID
     * @return ProcessInstanceModel ????????????????????????
     */
    public ProcessInstanceModel getProcessInstance(String processInstanceId) {
        long count = historyService.createHistoricProcessInstanceQuery().processInstanceId(processInstanceId).count();
        if(count == 0) return null;
        Optional<BusinessProcess> op = businessProcessRepository.findById(processInstanceId);
        BusinessProcess bp = op.isPresent()?op.get():null;
        return EntityModelUtil.toProcessInstanceModel(bp);
    }


    /**
     * ??????/??????????????????
     *
     * @author  zhaoyao
     * @param  processInstanceId ????????????ID
     * @return void
     */
    public void suspendProcessInstance(String processInstanceId, Boolean suspend) {
        Optional<BusinessProcess> op = businessProcessRepository.findById(processInstanceId);
        BusinessProcess bp = op.isPresent()?op.get():null;
        if(bp==null) throw new WorkflowException("???????????????????????????????????????????????????"+processInstanceId);
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
     * ????????????????????????
     *
     * @author  zhaoyao
     * @param  model ??????????????????????????????
     * @return List<ProcessInstanceModel> ???????????????????????????
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
     * ????????????????????????(??????)
     *
     * @author  zhaoyao
     * @param  model ??????????????????????????????
     * @return PageBean<ProcessInstanceModel> ????????????????????????
     */
    public PageBean<ProcessInstanceModel> findInstanceList(ProcessInstanceModel model, PageRequest pageRequest) {
        List<ProcessInstanceModel> instances = new ArrayList<>();
        Page<BusinessProcess> pageInstances = findByConditions(model,pageRequest);
        pageInstances.forEach(bp->{
            instances.add(EntityModelUtil.toProcessInstanceModel(bp));
        });
        PageBean page = new PageBean(pageInstances);
        page.setData(instances);
        return page;
    }


    //??????????????????
    public List<BusinessProcess> findByConditions(ProcessInstanceModel model) {
        CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
        CriteriaQuery<BusinessProcess> query = criteriaBuilder.createQuery(BusinessProcess.class);
        //Root ???????????????From???????????????????????????
        Root<BusinessProcess> root = query.from(BusinessProcess.class);
        //Predicate ??????????????????
        Predicate[] predicatesList = generatePredicates(model,root,criteriaBuilder);
        query.where(predicatesList);
        //?????????????????????ID??????
        query.orderBy( criteriaBuilder.desc(root.get("startTime")), criteriaBuilder.desc(root.get("processInstanceId")) );
        TypedQuery<BusinessProcess> typedQuery = entityManager.createQuery(query);
        return typedQuery.getResultList();
    }


    //??????????????????(??????)
    public Page<BusinessProcess> findByConditions(ProcessInstanceModel model, PageRequest pageRequest ) {
        Specification<BusinessProcess> specification = (Specification<BusinessProcess>) (root, criteriaQuery, criteriaBuilder) -> {
            //??????????????????
            Predicate[] predicates= generatePredicates(model,root,criteriaBuilder);
            //??????????????????(PageRequest????????????????????????????????????????????????)
            //criteriaQuery.orderBy( criteriaBuilder.desc(root.get("startTime")), criteriaBuilder.desc(root.get("processInstanceId")) );
            Predicate predicate = criteriaBuilder.and( predicates );
            return predicate;
        };
        return businessProcessRepository.findAll(specification, pageRequest);
    }


    //??????????????????
    private Predicate[] generatePredicates(ProcessInstanceModel model,Root<BusinessProcess> root, CriteriaBuilder criteriaBuilder){
        List<Predicate> predicatesList = new ArrayList<>();
        if (StringUtils.isNotBlank(model.getProcessInstanceName())) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("processInstanceName"), "%" + model.getProcessInstanceName() + "%"));
            predicatesList.add(predicate);
        }
        if (StringUtils.isNotBlank(model.getProcessDefinitionName())) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("processDefinitionName"), "%" + model.getProcessDefinitionName() + "%"));
            predicatesList.add(predicate);
        }
        if (StringUtils.isNotBlank(model.getBusinessName())) {
            Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("businessName"), "%" + model.getBusinessName() + "%"));
            predicatesList.add(predicate);
        }
        if (StringUtils.isNotBlank(model.getDeploymentId())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("deploymentId"),  model.getDeploymentId()) )  );
        }
        if (StringUtils.isNotBlank(model.getBusinessId())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("businessId"),  model.getBusinessId()) )  );
        }
        if (StringUtils.isNotBlank(model.getBusinessType())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("businessType"),  model.getBusinessType()) )  );
        }
        if (StringUtils.isNotBlank(model.getProcessInstanceId())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("processInstanceId"),  model.getProcessInstanceId()) )  );
        }
        if (StringUtils.isNotBlank(model.getStartUserId())) {
            predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("startUserId"),  model.getStartUserId()) )  );
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
