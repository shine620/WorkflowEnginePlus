package com.hy.workflow.service;

import com.hy.workflow.base.FindNextActivityCmd;
import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.*;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.repository.ProcessDefinitionConfigRepository;
import com.hy.workflow.util.EntityModelUtil;
import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ActivityInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
@Transactional
public class FlowableTaskService {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private ManagementService managementService;

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;

    @Autowired
    private ProcessDefinitionConfigRepository processDefinitionConfigRepository;

    @Autowired
    private ProcessDefinitionService processDefinitionService;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;


    /**
     * 审批任务
     *
     * @author  zhaoyao
     * @param approveRequest 审批请求数据
     * @return List<FlowElementModel>
     */
    public void completeTask(ApproveRequest approveRequest) {
        ApproveType approveType = approveRequest.getApproveType();
        //生成审批意见
        taskService.addComment(approveRequest.getTaskId(), approveRequest.getProcessInstanceId(), approveRequest.getOpinion());
        //审批通过
        if(ApproveType.APPROVE.equals(approveType)){

            //设置下一环节处理信息
            Map<String,Object> variables = approveRequest.getVariables();
            WorkflowUtil.setNextTaskInfoVariables(variables,approveRequest);

            //会签非最后一个实例时不设置下环节处理人信息
            Task task = taskService.createTaskQuery().taskId(approveRequest.getTaskId()).singleResult();
            Map<String,Object> processVariables = runtimeService.getVariables(task.getExecutionId());
            if(processVariables.get("nrOfInstances")!=null){
                int nrOfInstances = (Integer) processVariables.get("nrOfInstances");
                int nrOfCompletedInstances = (Integer) processVariables.get("nrOfCompletedInstances");
                if( nrOfInstances!=0 && nrOfCompletedInstances != nrOfInstances -1){
                    taskService.complete(approveRequest.getTaskId(),variables);
                    return;
                }
            }

            //所选择的流程分支节点
            List<String> selectOutNode = new ArrayList<>();
            approveRequest.getNextTaskList().forEach(nextTask ->{
                selectOutNode.add(nextTask.getGroupId());
            });

            //当前节点信息
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            FlowNode currentNode = (FlowNode) bpmnModel.getFlowElement(execution.getActivityId());

            //剪断当前节点未选择的下一分支流向
            List<SequenceFlow> removedNodes = new ArrayList<>();
            List<SequenceFlow> outLines = currentNode.getOutgoingFlows();
            Iterator<SequenceFlow> it = outLines.listIterator();
            while (it.hasNext()){
                SequenceFlow sequenceFlow = it.next();
                FlowElement target = sequenceFlow.getTargetFlowElement();
                if(!selectOutNode.contains(target.getId())){
                    removedNodes.add( sequenceFlow );
                    it.remove();
                }
            }

            //审批任务
            taskService.complete(approveRequest.getTaskId(),variables);

            //审批完成后还原原来的分支流向
            outLines.addAll(removedNodes);
        }
        //驳回  TODO
        else if(ApproveType.REJECT.equals(approveType)){


        }
        //转办
        else if(ApproveType.TURN.equals(approveType)){
        }
        //委托
        else if(ApproveType.ENTRUST.equals(approveType)){
        }
    }


    /**
     * 获取第一个审批节点
     *
     * @author  zhaoyao
     * @param processDefinitionId 流程定义ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getFirstNode(String processDefinitionId) {

        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if(model == null)  throw new WorkflowException("该流程定义没有模型数据："+processDefinitionId);

        List<FlowElementModel> flowList =  new ArrayList<>();
        List<String> flowIdList =  new ArrayList<>();

        Process process =  model.getMainProcess();
        FlowElement startNode = process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) ((StartEvent) startNode).getOutgoingFlows().get(0).getTargetFlowElement();
        List<SequenceFlow> outgoingFlows = firstNode.getOutgoingFlows();

        for (SequenceFlow outgoingFlow : outgoingFlows) {

            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
            FlowElementModel flow = new FlowElementModel();

            //用户任务（包含会签）
            if (targetFlowElement instanceof UserTask) {
                UserTask userTask =  (UserTask)targetFlowElement;
                flow.setId(userTask.getId());
                flow.setName(userTask.getName());
                MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                if(multiInstance!=null){
                    if(multiInstance.isSequential()) flow.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                    else flow.setFlowElementType(FlowElementType.PARALLEL_TASK);
                }else{
                    flow.setFlowElementType(FlowElementType.USER_TASK);
                }
                flowList.add(flow);
                flowIdList.add(flow.getId());
            }
            //子流程
            else if(targetFlowElement instanceof SubProcess){
                SubProcess subProcess = (SubProcess)targetFlowElement;
                UserTask userTask = WorkflowUtil.getSubProcessFirstTask(subProcess,true);
                if(userTask!=null){
                    flow.setId(userTask.getId());
                    flow.setName(userTask.getName());
                    flow.setFlowElementType(FlowElementType.USER_TASK);
                    flow.setParentId( subProcess.getId() );
                    flow.setParentName( subProcess.getName() );
                    flow.setParentType( FlowElementType.SUB_PROCESS);
                    flowList.add(flow);
                    flowIdList.add(flow.getId());
                }
            }
            //调用活动
            else if(targetFlowElement instanceof CallActivity){
                CallActivity callActivity =  (CallActivity)targetFlowElement;
                flow.setId(callActivity.getId());
                flow.setName(callActivity.getName());
                flow.setFlowElementType(FlowElementType.CALL_ACTIVITY);
                flowList.add(flow);
                flowIdList.add(flow.getId());
            }
            //其他类型节点
            else{
                throw new WorkflowException("第二节点只能为用户任务节点或者子流程！");
            }
        }

        //设置节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);

        return flowList;
    }


    /**
     * 根据当前任务ID查询下一个审批节点
     *
     * @author  zhaoyao
     * @param taskId 任务ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getNextFlowNode(String taskId) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List<FlowNode> activityList = managementService.executeCommand(new FindNextActivityCmd(execution, bpmnModel));

        List<FlowElementModel> flowList = new ArrayList<>();
        List<String> flowIdList =  new ArrayList<>();

        //最后一个会签环实例才可以选择下一节点处理人信息(非固定人员时可选)
        Map<String,Object> variables = runtimeService.getVariables(task.getExecutionId());
        if(variables.get("nrOfInstances")!=null){
            int nrOfInstances = (Integer) variables.get("nrOfInstances");
            int nrOfCompletedInstances = (Integer) variables.get("nrOfCompletedInstances");
            if( nrOfInstances!=0 && nrOfCompletedInstances != nrOfInstances -1) return flowList;
        }

        activityList.forEach( flowNode -> {

            FlowElementModel model =  new FlowElementModel();
            model.setId(flowNode.getId());
            model.setName(flowNode.getName());

            //普通任务及嵌入式子流程
            if(flowNode instanceof UserTask){
                UserTask userTask = (UserTask)flowNode;
                MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                if(multiInstance!=null){
                    if(multiInstance.isSequential()) model.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                    else model.setFlowElementType(FlowElementType.PARALLEL_TASK);
                }else{
                    model.setFlowElementType(FlowElementType.USER_TASK);
                }
                //属于子流程任务节点时设置Parent和分组
                FlowElementsContainer container = flowNode.getParentContainer();
                if(container instanceof SubProcess){
                    model.setParentId( ((SubProcess)container).getId() );
                    model.setParentName( ((SubProcess) container).getName() );
                    setFlowElementGroup((SubProcess)container,execution.getActivityId(),model);
                }else{
                    //设置任务组信息(下一环节有多个时需要选择，如一条线连到普通任务节点，另一条线连到并行网关分支，根据分组选择)
                    setFlowElementGroup(flowNode,execution.getActivityId(),model);
                }
            }
            //调用活动
            else if(flowNode instanceof CallActivity){
                model.setFlowElementType(FlowElementType.CALL_ACTIVITY);
                setFlowElementGroup(flowNode,execution.getActivityId(),model);
            }
            //结束
            else if(flowNode instanceof EndEvent){
                model.setFlowElementType(FlowElementType.END_EVENT);
                if(StringUtils.isBlank(model.getName())) model.setName("结束");
                setFlowElementGroup(flowNode,execution.getActivityId(),model);
            }
            else {
                throw new WorkflowException("不支持的节点类型：processDefinitionId="+task.getProcessDefinitionId()+"  flowElementId="+task.getTaskDefinitionKey());
            }

            flowList.add(model);
            flowIdList.add(flowNode.getId());

        });
        //查询并填充节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);

        return flowList;

    }


    /**
     * 任务节点分组
     *
     * @author  zhaoyao
     * @param flowNode 节点信息
     * @param rootId 根节点 ID
     * @param model 分组信息要封装到的对象
     */
    private void setFlowElementGroup(FlowNode flowNode, String rootId, FlowElementModel model){
        List<SequenceFlow> incomingFlows = flowNode.getIncomingFlows();
        for(SequenceFlow line : incomingFlows){
            FlowElement sourceFlow = line.getSourceFlowElement();
            if(sourceFlow.getId().equals(rootId)){
                model.setGroupId(flowNode.getId());
                if(StringUtils.isNotBlank(flowNode.getName())){
                    model.setGroupName(flowNode.getName());
                }
                else{
                    if(flowNode instanceof ExclusiveGateway) model.setGroupName("互斥网关任务");
                    else if(flowNode instanceof ParallelGateway) model.setGroupName("并行网关任务");
                    else if(flowNode instanceof InclusiveGateway) model.setGroupName("包含网关任务");
                    else if(flowNode instanceof EndEvent) model.setGroupName("结束");
                    else model.setGroupName("其他任务");
                }
                return;
            }else if(sourceFlow instanceof Gateway){
                setFlowElementGroup((FlowNode)sourceFlow,rootId,model);
            }
        }
    }


    /**
     * 根据部门ID获取该部门子流程审批节点
     *
     * @author  zhaoyao
     * @param departmentId 部门ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getSubProcessByDeptId(String departmentId) {
        List<FlowElementModel> nodeList = new ArrayList<>();
        ProcessDefinitionConfigModel model = new ProcessDefinitionConfigModel();
        model.setDepartmentId(departmentId);
        //获取该单位所有的流程定义配置
        List<ProcessDefinitionConfigModel> modelConfigList = processDefinitionService.findProcessDefinitionConfigLaterstList(model);
        modelConfigList.forEach( modelConfig -> {
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionId(modelConfig.getProcessDefinitionId()).singleResult();
            //查找子流程第一个节点及该节点配置
            Process subProcess =  repositoryService.getBpmnModel(processDefinition.getId()).getMainProcess();
            StartEvent subStartEvent = (StartEvent) subProcess.getInitialFlowElement();
            FlowNode subFirstNode =(FlowNode) subStartEvent.getOutgoingFlows().get(0).getTargetFlowElement();
            FlowElementConfig subFirstNodeConfig = flowElementConfigRepository.findByProcessDefinitionIdAndFlowElementId(processDefinition.getId(),subFirstNode.getId());
            FlowElementConfigModel configModel = EntityModelUtil.toFlowElementConfigMode(subFirstNodeConfig);
            //封装任务节点
            FlowElementModel node = new FlowElementModel();
            node.setId(subFirstNode.getId());
            node.setName(subFirstNode.getName());
            MultiInstanceLoopCharacteristics multiInstance = ((UserTask)subFirstNode).getLoopCharacteristics();
            if(multiInstance!=null){
                if(multiInstance.isSequential()) node.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                else node.setFlowElementType(FlowElementType.PARALLEL_TASK);
            }else{
                node.setFlowElementType(FlowElementType.USER_TASK);
            }
            node.setFlowElementType(FlowElementType.USER_TASK);
            node.setConfig(configModel);
            node.setModelKey(processDefinition.getKey());
            node.setDepartmentId(departmentId);
            nodeList.add(node);
        });
        return nodeList;
    }


    /**
     * 根据用户ID查询待办信息列表
     *
     * @author  zhaoyao
     * @param loadAll 是否查询全部
     * @param pageNum 页码
     * @param pageSize 每页大小
     * @param userId 用户ID
     * @return PageBean<TaskModel>
     */
    @Transactional(propagation= Propagation.NOT_SUPPORTED)
    public PageBean<TaskModel>  getTodoTaskList(Boolean loadAll, Integer pageNum, Integer pageSize, String userId) {
        ArrayList<TaskModel> taskList =  new ArrayList();
        TaskQuery taskQuery  = taskService.createTaskQuery().taskCandidateOrAssigned(userId).orderByTaskCreateTime().desc();
        Long totalCount = taskQuery.count();

        List<Task> todoTaskList ;
        if(loadAll==true){
            todoTaskList = taskQuery.list();
        }else{
            int startIndex = (pageNum-1)*20;
            todoTaskList = taskQuery.listPage(startIndex,pageSize);
        }

        Set<String> instanceIds = new HashSet<>();
        todoTaskList.forEach(task -> {
            TaskModel taskModel = new TaskModel();
            taskModel.setTaskId(task.getId());
            taskModel.setTaskName(task.getName());
            taskModel.setTaskDefinitionKey(task.getTaskDefinitionKey());
            taskModel.setProcessInstanceId(task.getProcessInstanceId());
            taskModel.setProcessDefinitionId(task.getProcessDefinitionId());
            taskModel.setCreateTime(task.getCreateTime());
            taskModel.setClaimTime(task.getClaimTime());
            taskModel.setAssignee(task.getAssignee());
            taskModel.setOwner(task.getOwner());
            taskModel.setExecutionId(task.getExecutionId());
            taskList.add(taskModel);
            instanceIds.add(task.getProcessInstanceId());
        });

        //查询流程实例相关信息
        List<BusinessProcess> businessList = businessProcessRepository.findAllById(instanceIds);
        Map<String,BusinessProcess> bsMap = new HashMap<>();
        businessList.forEach(businessProcess -> {
            bsMap.put(businessProcess.getProcessInstanceId(),businessProcess);
        });

       //设置流程实例名称
        taskList.forEach(taskModel -> {
            businessList.forEach(businessProcess -> {
                BusinessProcess bp = bsMap.get(taskModel.getProcessInstanceId());
                taskModel.setProcessInstanceName(bp.getProcessInstanceName());
            });
        });

        PageBean taskPage = new PageBean(pageNum,pageSize,totalCount);
        taskPage.setData(taskList);

        return taskPage;
    }


    /**
     * 查询待办任务信息
     *
     * @author  zhaoyao
     * @param taskId 任务ID
     * @return TaskModel
     */
    public TaskModel todoTaskInfo(String taskId) {
        TaskModel model = null;
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if(task!=null){
            model = new TaskModel();
            model.setTaskId(task.getId());
            model.setTaskName(task.getName());
            model.setTaskDefinitionKey(task.getTaskDefinitionKey());
            model.setProcessInstanceId(task.getProcessInstanceId());
            model.setProcessDefinitionId(task.getProcessDefinitionId());
            model.setAssignee(task.getAssignee());
            model.setOwner(task.getOwner());
            model.setCreateTime(task.getCreateTime());
            model.setClaimTime(task.getClaimTime());
            model.setExecutionId(task.getExecutionId());
        }
        //流程实例和业务信息
        Optional<BusinessProcess> optional = businessProcessRepository.findById(task.getProcessInstanceId());
        BusinessProcess bp = optional.isPresent()?optional.get():null;
        model.setProcessInstanceName(bp.getProcessInstanceName());
        //候选用户及候选组信息
        List<String> candidateUsers = new ArrayList<>();
        List<String> candidateGroups =  new ArrayList<>();
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        for (IdentityLink identityLink : identityLinks) {
            if ( IdentityLinkType.CANDIDATE.equalsIgnoreCase(identityLink.getType()))  {
                if (StringUtils.isNotBlank(identityLink.getUserId())) {
                    candidateUsers.add(identityLink.getUserId());
                } else if (StringUtils.isNotBlank(identityLink.getGroupId())) {
                    candidateGroups.add(identityLink.getGroupId());
                }
            }
        }

        Map<String,Object> variables = runtimeService.getVariables(task.getExecutionId());
        if(variables.get("nrOfInstances")!=null){
            model.setNrOfInstances((Integer) variables.get("nrOfInstances"));
            model.setNrOfCompletedInstances((Integer) variables.get("nrOfCompletedInstances"));
        }

        model.setCandidateUsers(candidateUsers);
        model.setCandidateGroups(candidateGroups);
        return model;
    }


}
