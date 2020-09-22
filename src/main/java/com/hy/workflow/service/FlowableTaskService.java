package com.hy.workflow.service;


import com.hy.workflow.base.EvaluateExpressionCmd;
import com.hy.workflow.base.FindNextActivityCmd;
import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.RejectRecord;
import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.enums.RejectPosition;
import com.hy.workflow.enums.RejectType;
import com.hy.workflow.model.*;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.repository.RejectRecordRepository;
import com.hy.workflow.util.EntityModelUtil;
import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ChangeActivityStateBuilder;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


import java.util.*;

@Service
@Transactional
public class FlowableTaskService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableTaskService.class);

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
    private ProcessDefinitionService processDefinitionService;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private RejectRecordRepository rejectRecordRepository;


    /**
     * 审批任务
     *
     * @author  zhaoyao
     * @param approveRequest 审批请求数据
     * @return List<FlowElementModel>
     */
    public void completeTask(ApproveRequest approveRequest) {

        ApproveType approveType = approveRequest.getApproveType();
        taskService.addComment(approveRequest.getTaskId(), approveRequest.getProcessInstanceId(), approveRequest.getOpinion());//生成审批意见

         /** 审批通过 */
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
            if(approveRequest.getNextTaskList()!=null){
                approveRequest.getNextTaskList().forEach(nextTask ->{
                    selectOutNode.add(nextTask.getGroupId());
                });
            }

            //当前节点信息
            ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
            FlowNode currentNode = (FlowNode) bpmnModel.getFlowElement(execution.getActivityId());

            //任务未签收时先进行签收
            if(task.getAssignee()==null&&approveRequest.getUserId()!=null){
                taskService.claim(task.getId(),approveRequest.getUserId());
            }

            //审批流程节点
            WorkflowUtil.completeTaskBySelectNode(selectOutNode,currentNode,taskService,task,variables);

        }
        /** 驳回 */
        else if(ApproveType.REJECT.equals(approveType)){
            rejectTask(approveRequest);
        }
        /** 转办 */
        else if(ApproveType.TURN.equals(approveType)){
        }
        /** 委托 */
        else if(ApproveType.ENTRUST.equals(approveType)){
        }

    }


    /**
     * 驳回任务
     *
         拆分原子标签：①普通用户任务  ②会签用户任务  ③会签子流程  ④并行普通用户任务  ⑤并行会签用户任务  ⑥并行会签子流程任务
         排列组合：以主流程的角度出发
         1. ① 非并行分支上普通用户任务
         2. ② 非并行分支上会签用户任务
         3. ③① 非并行分支上会签子流程，对应子流程任务为普通用户任务
         4. ③② 非并行分支上会签子流程，对应子流程任务为会签用户任务
         5. ③④ 非并行分支上会签子流程，对应子流程任务为并行普通用户任务
         6. ③⑤ 非并行分支上会签子流程，对应子流程任务为并行会签用户任务
         7. ④ 并行普通用户任务
         8. ⑤ 并行会签用户任务
         9. ⑥① 并行分支上会签子流程，对应子流程任务为普通用户任务
         10.⑥② 并行分支上会签子流程，对应子流程任务为会签用户任务
         11.⑥④ 并行分支上会签子流程，对应子流程任务为会并行普通用户任务
         12.⑥⑤ 并行分支上会签子流程，对应子流程任务为会并行会签用户任务

     * @author  zhaoyao
     * @param approveRequest 审批请求数据
     */
    private void rejectTask(ApproveRequest approveRequest){

        Task task = taskService.createTaskQuery().taskId(approveRequest.getTaskId()).singleResult();
        ProcessInstance instance = runtimeService.createProcessInstanceQuery().processInstanceId(task.getProcessInstanceId()).singleResult();
        if(instance == null) throw new WorkflowException("驳回失败，流程信息不存在：ProcessInstanceId="+task.getProcessInstanceId());
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        FlowNode currentNode = (FlowNode) bpmnModel.getFlowElement(task.getTaskDefinitionKey());

        ApproveRequest.RejectInfo rejectInfo = approveRequest.getRejectInfo();
        String parentProcessInstanceId = rejectInfo.getParentProcessInstanceId();
        String targetNodeId = rejectInfo.getFlowElementId();
        String rejectPosition = rejectInfo.getRejectPosition();

        //当前任务执行实例
        ExecutionEntity taskExe = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        //当前任务实例父实例（执行实例或者流程实例）
        ExecutionEntity parentExe =  (ExecutionEntity) runtimeService.createExecutionQuery().executionId(taskExe.getParentId()).singleResult();
        String processInstanceId = taskExe.getProcessInstanceId();
        String rootProcessInstanceId = taskExe.getRootProcessInstanceId();

        ChangeActivityStateBuilder changeBuilder = runtimeService.createChangeActivityStateBuilder();
        List<Execution> processList=  runtimeService.createExecutionQuery().parentId(rootProcessInstanceId).list();
        logger.info("流程驳回操作：processInstanceId:{}，taskId:{}，rejectNode:{}",task.getProcessInstanceId(),task.getId(),targetNodeId);

        /* 用户任务：无子流程、无会签 */
        if(StringUtils.equals(processInstanceId,rootProcessInstanceId)&&StringUtils.equals(processInstanceId,taskExe.getParentId())){
            /** ①普通用户任务 */
            if(processList.size()==1)
                changeBuilder.processInstanceId(task.getProcessInstanceId()).moveActivityIdTo(task.getTaskDefinitionKey(),targetNodeId).changeState();
            /** ④并行分支上普通用户任务 */
            else if(processList.size()>1)
                innerReject(changeBuilder,task,targetNodeId,rejectPosition);
        }
        else{
            /* 用户任务会签 */
            if( StringUtils.equals(parentExe.getParentId(),parentExe.getRootProcessInstanceId()) ){
                /** ②会签用户任务 */
                if(processList.size()==1)
                    changeBuilder.processInstanceId(task.getProcessInstanceId()).moveActivityIdTo(task.getTaskDefinitionKey(),targetNodeId).changeState();
                /** ⑤并行分支上会签用户任务 */
                else if(processList.size()>1)
                    innerReject(changeBuilder,task,targetNodeId,rejectPosition);
            }
            /* 子流程会签 */
            else{
                /**③①  ③④  ⑥①  ⑥④ */
                if(parentExe.getSuperExecutionId()!=null){ //
                    //当前子流程是否有多个用户任务
                    List<Execution> executionList=  runtimeService.createExecutionQuery().parentId(parentExe.getId()).list();
                    //当前子流程会签任务对应其父流程上的调用活动节点执行实例
                    ExecutionEntity superExe = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(parentExe.getSuperExecutionId()).singleResult();
                    //子流程的任务驳回
                    subProcessReject(changeBuilder,task,executionList,superExe,targetNodeId,rejectPosition,parentProcessInstanceId);
                }
                /** ③②  ③⑤  ⑥②  ⑥⑤ */
                else{
                    //当前子流程是否有多个会签任务
                    List<Execution> executionList=  runtimeService.createExecutionQuery().parentId(parentExe.getProcessInstanceId()).list();
                    //当前子流程会签任务对应其父流程上的调用活动节点执行实例
                    ExecutionEntity grandpaExe = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(parentExe.getParentId()).singleResult();
                    ExecutionEntity superExe = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(grandpaExe.getSuperExecutionId()).singleResult();
                    //子流程的任务驳回
                    subProcessReject(changeBuilder,task,executionList,superExe,targetNodeId,rejectPosition,parentProcessInstanceId);
                }
            }

        }

        //记录驳回信息
        RejectRecord rejectRecord = new RejectRecord();
        String businessKey = instance.getBusinessKey();
        rejectRecord.setProcessInstanceId(task.getProcessInstanceId());
        rejectRecord.setBusinessType(businessKey.split(";")[0]);
        rejectRecord.setBusinessId(businessKey.split(";")[1]);
        rejectRecord.setSourceTaskId(task.getId());
        rejectRecord.setSourceElementId(currentNode.getId());
        rejectRecord.setSourceElementName(currentNode.getName());
        rejectRecord.setRejectUser(approveRequest.getUserId());
        if(RejectType.RESUBMIT.equals(rejectInfo.getRejectType()))
            rejectRecord.setRejectType(RejectType.RESUBMIT);
        else if(RejectType.SUBMIT_REJECT.equals(rejectInfo.getRejectType()))
            rejectRecord.setRejectType(RejectType.SUBMIT_REJECT);
        rejectRecord.setTargetElementId(rejectInfo.getFlowElementId());
        rejectRecord.setTargetElementName(rejectInfo.getFlowElementName());
        rejectRecord.setTargetProcessInstanceId(rejectInfo.getParentProcessInstanceId());
        rejectRecord.setCreateTime(new Date());
        rejectRecordRepository.save(rejectRecord);
    }


    //存在子流程会签的驳回
    private void subProcessReject(ChangeActivityStateBuilder changeBuilder,Task task,List<Execution> executionList,ExecutionEntity superExe,String targetNodeId,String rejectPosition,String parentProcessInstanceId){
        //当前子流程任务对应其父流程上的调用活动节点，是否存在多个并行任务
        List<Execution> superParentExeList=  runtimeService.createExecutionQuery().parentId(superExe.getProcessInstanceId()).list();
        if( superParentExeList.size()==1&& executionList.size()==1 ){
            /** ③①非并行分支上会签子流程，对应子流程任务为普通用户任务
             *   ③②非并行分支上会签子流程，对应子流程任务为会签用户任务*/
            if(parentProcessInstanceId!=null){   //驳回到父流程
                rejectParentProcess(changeBuilder,parentProcessInstanceId,superExe,targetNodeId);
            }else{   //子流程内部驳回
                changeBuilder.processInstanceId(task.getProcessInstanceId()).moveActivityIdTo(task.getTaskDefinitionKey(),targetNodeId).changeState();
            }
        }
        else if( superParentExeList.size()==1&& executionList.size()>1 ){
            /** ③④非并行分支上会签子流程，对应子流程任务为并行普通用户任务
             *   ③⑤非并行分支上会签子流程，对应子流程任务为并行会签用户任务 */
            if(parentProcessInstanceId!=null){   //驳回到父流程
                rejectParentProcess(changeBuilder,parentProcessInstanceId,superExe,targetNodeId);
            }else{   //子流程内部驳回(并行分支)
                innerReject(changeBuilder,task,targetNodeId,rejectPosition);
            }
        }else if( superParentExeList.size()>1&& executionList.size()==1 ){
            /** ⑥①并行分支上会签子流程，对应子流程任务为普通用户任务
             *   ⑥②并行分支上会签子流程，对应子流程任务为会签用户任务 */
            if(parentProcessInstanceId!=null){   //驳回到父流程
                rejectParentProcess(changeBuilder,parentProcessInstanceId,superExe,superParentExeList,targetNodeId,rejectPosition);
            }else{   //子流程内部驳回
                changeBuilder.processInstanceId(task.getProcessInstanceId()).moveActivityIdTo(task.getTaskDefinitionKey(),targetNodeId).changeState();
            }
        }else if( superParentExeList.size()>1&& executionList.size()>1 ){
            /** ⑥④并行分支上会签子流程，对应子流程任务为会并行普通用户任务
             *   ⑥⑤并行分支上会签子流程，对应子流程任务为会并行会签用户任务*/
            if(parentProcessInstanceId!=null){   //驳回到父流程
                rejectParentProcess(changeBuilder,parentProcessInstanceId,superExe,superParentExeList,targetNodeId,rejectPosition);
            }else{   //子流程内部驳回(并行分支)
                innerReject(changeBuilder,task,targetNodeId,rejectPosition);
            }
        }
    }

    //并行分支流程内部驳回(主流程内部驳回/子流程内部驳回)
    private void innerReject(ChangeActivityStateBuilder changeBuilder,Task task,String targetNodeId,String rejectPosition){
        if(RejectPosition.AFTER_GATEWAY.equals(rejectPosition)||RejectPosition.NO_GATEWAY.equals(rejectPosition)){
            changeBuilder.processInstanceId(task.getProcessInstanceId()).moveActivityIdTo(task.getTaskDefinitionKey(),targetNodeId).changeState();
        }else if(RejectPosition.BEFORE_GATEWAY.equals(rejectPosition)){
            ProcessDefinitionConfigModel definitionConfig = processDefinitionService.getProcessConfig(task.getProcessDefinitionId());
            if(definitionConfig.getRejectGatewayBefore()!=null&&definitionConfig.getRejectGatewayBefore()){
                List<String > currentExecutionIds = new ArrayList<>();
                List<Execution> executions = runtimeService.createExecutionQuery().parentId(task.getProcessInstanceId()).list();
                for (Execution execution : executions) {
                    currentExecutionIds.add(execution.getId());
                }
                changeBuilder.moveExecutionsToSingleActivityId(currentExecutionIds, targetNodeId).changeState();
            }else{
                throw new WorkflowException("此流程不允许驳回到网关发起前节点：processInstanceId="+task.getProcessInstanceId()+",taskId="+task.getId());
            }
        }
    }

    //子流程驳回到主流程
    private void rejectParentProcess(ChangeActivityStateBuilder changeBuilder, String parentProcessInstanceId, Execution superExe, String targetNodeId){
        ProcessInstance parentInstance = runtimeService.createProcessInstanceQuery().processInstanceId(parentProcessInstanceId).singleResult();
        ProcessDefinitionConfigModel definitionConfig = processDefinitionService.getProcessConfig(parentInstance.getProcessDefinitionId());
        if(definitionConfig.getRejectParentProcess()!=null&&definitionConfig.getRejectParentProcess()){
            changeBuilder.moveExecutionsToSingleActivityId( Collections.singletonList(superExe.getParentId()), targetNodeId ).changeState();
        }
    }

    //子流程驳回到主流程(子流程处在父流程并行网关分支中)
    private void rejectParentProcess(ChangeActivityStateBuilder changeBuilder, String parentProcessInstanceId, Execution superExe, List<Execution> superParentExeList, String targetNodeId, String rejectPosition){
        ProcessInstance parentInstance = runtimeService.createProcessInstanceQuery().processInstanceId(parentProcessInstanceId).singleResult();
        ProcessDefinitionConfigModel definitionConfig = processDefinitionService.getProcessConfig(parentInstance.getProcessDefinitionId());
        if(definitionConfig.getRejectParentProcess()!=null&&definitionConfig.getRejectParentProcess()){
            /*是否驳回到父流程网关前节点（如果父流程上对应的调用活动节点在并行网关分支中，子流程应该只能驳回到父流程上发起子流程的节点，
               不应能驳回到父流程网关前节点，这样会清空父流程上其他网关分支上的节点），目前系统设计驳回到父流程时rejectPosition值不会为BEFORE_GATEWAY*/
            if(RejectPosition.BEFORE_GATEWAY.equals(rejectPosition)){
                List<String > executionIds = new ArrayList<>();
                for(Execution execution : superParentExeList){
                    executionIds.add(execution.getId());
                }
                changeBuilder.moveExecutionsToSingleActivityId( executionIds, targetNodeId ).changeState();
            }else{ //rejectPosition=(null,AFTER_GATEWAY,NO_GATEWAY)
                changeBuilder.moveExecutionsToSingleActivityId( Collections.singletonList(superExe.getParentId()), targetNodeId ).changeState();
            }

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
            flow.setGroupId(targetFlowElement.getId());
            flow.setGroupName(targetFlowElement.getName());

            //用户任务（包含会签）
            if (targetFlowElement instanceof UserTask) {
                UserTask userTask =  (UserTask)targetFlowElement;
                flow.setId(userTask.getId());
                flow.setName(userTask.getName());
                flow.setFlowElementType(getUserTaskType(userTask));
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
                    flow.setGroupId(userTask.getId());
                    flow.setGroupName(userTask.getName());
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
        //TODO 还需处理 并行用户任务会签、子流程会签、并行子流程会签
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
                model.setFlowElementType(getUserTaskType(userTask));
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
            node.setFlowElementType( getUserTaskType((UserTask)subFirstNode) );
            node.setConfig(configModel);
            node.setModelKey(processDefinition.getKey());
            node.setModelName(processDefinition.getName());
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


    /**
     * 查询任务可驳回节点
     *
     * 流程驳回的基本规则：
     * 1.审批过的历史节点
     * 2.若存在并行或者包含网关节点，在同一条分支线上可驳回
     * 3.若存在并行或者包含网关节点，且当前节点在网关合并之后，则不能驳回到网关上的某个节点，只能驳回到网关分支发起之前的节点
     *
     * @author  zhaoyao
     * @param taskId 任务ID
     * @return RejectTask
     */
    public RejectTask getRejectTask(String taskId) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity execution = (ExecutionEntity)runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        RejectTask rejectTask = new RejectTask();
        rejectTask.setTaskId(task.getId());
        rejectTask.setTaskName(task.getName());
        rejectTask.setTaskDefinitionKey(task.getTaskDefinitionKey());

        //审批过的历史节点ID
        ArrayList<String> hisTaskKeys = new ArrayList<>();
        List<HistoricTaskInstance> hisTaskList = historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(task.getProcessInstanceId()).finished()
                .orderByHistoricTaskInstanceEndTime().desc()
                .list();
        hisTaskList.forEach(historicTaskInstance -> {
            hisTaskKeys.add(historicTaskInstance.getTaskDefinitionKey());
        });

        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        Process process =  bpmnModel.getMainProcess();
        StartEvent startEvent = (StartEvent) process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) (startEvent).getOutgoingFlows().get(0).getTargetFlowElement();
        Map<String,FlowElement> elementMap = bpmnModel.getMainProcess().getFlowElementMap();
        FlowNode currentNode= (FlowNode) elementMap.get(task.getTaskDefinitionKey());

        //可驳回的历史审批节点(若存在并行网关，则与当前节点在同一条分支线上，不包括网关前的节点)
        Map<String, FlowElement> afterGatewayMap = new LinkedHashMap<>();
        afterGatewayHisTask(hisTaskKeys,currentNode,afterGatewayMap,execution);

        //并行或包含网关之前的可驳回历史节点
        Map<String, FlowElement> beforeGatewayMap = new LinkedHashMap<>();
        //检查流程配置是否允许并行网关分支中节点驳回网关发起前
        ProcessDefinitionConfigModel definitionConfig = processDefinitionService.getProcessConfig(task.getProcessDefinitionId());
        if(definitionConfig.getRejectGatewayBefore()!=null&&definitionConfig.getRejectGatewayBefore()){
            if(!firstNode.getId().equals(currentNode.getId())) beforeGatewayMap.put(firstNode.getId(),firstNode);
            beforeGatewayHisTask(hisTaskKeys,firstNode,beforeGatewayMap,execution);
        }

        //网关前和网关节点后有交集，说明非并行网关节点
        //Set<String> differenceSet = Sets.intersection(afterGatewayMap.keySet(), beforeGatewayMap.keySet());

        //可驳回节点
        Map<String, FlowElement> rejectable = new LinkedHashMap<>();
        rejectable.putAll(beforeGatewayMap);
        rejectable.putAll(afterGatewayMap);

        //封装返回数据
        List<RejectTask.TaskNode> rejectNodes = new ArrayList<>();
        rejectable.forEach((id, flowElement) -> {
            RejectTask.TaskNode node = new RejectTask.TaskNode();
            node.setFlowElementId(id);
            node.setFlowElementName(flowElement.getName());
            if(flowElement instanceof UserTask){ //设置节点类型
                node.setFlowElementType(getUserTaskType((UserTask)flowElement));
            }else if(flowElement instanceof CallActivity){
                node.setFlowElementType(FlowElementType.CALL_ACTIVITY);
            }
            if( afterGatewayMap.containsKey(id)&&beforeGatewayMap.containsKey(id) ){
                node.setRejectPosition(RejectPosition.NO_GATEWAY);
            }else if(afterGatewayMap.containsKey(id)){
                node.setRejectPosition(RejectPosition.AFTER_GATEWAY);
            }else if(beforeGatewayMap.containsKey(id)){
                node.setRejectPosition(RejectPosition.BEFORE_GATEWAY);
            }
            rejectNodes.add(node);
        });

        //父流程上可驳回的流程节点
        rejectNodes.addAll( getParentRejectableNode(task.getProcessInstanceId()) );

        rejectTask.setHisTask(rejectNodes);

        return rejectTask;
    }


    //并行或包含网关之前的可驳回历史节点，正向查找
    private void beforeGatewayHisTask(ArrayList<String> hisTaskKeys, FlowNode currentNode, Map<String, FlowElement> map,ExecutionEntity execution){
        List<SequenceFlow> outLines = currentNode.getOutgoingFlows();
        for(SequenceFlow sequenceFlow : outLines ) {
            //排他网关判断条件
            if(currentNode instanceof ExclusiveGateway){
                boolean exprResult = managementService.executeCommand(new EvaluateExpressionCmd(sequenceFlow,execution));
                if (!exprResult) continue;
            }
            FlowElement targetFlow = sequenceFlow.getTargetFlowElement();
            //嵌入式子流程
            if(targetFlow instanceof SubProcess ){
                Map<String,FlowElement> subNodes = WorkflowUtil.getSubProcessRejectableTask(hisTaskKeys, (SubProcess) targetFlow);
                map.putAll(subNodes);
                beforeGatewayHisTask(hisTaskKeys, (FlowNode) targetFlow,map,execution);
            }
            //调用活动
            else if(targetFlow instanceof CallActivity){
                map.put(targetFlow.getId(),targetFlow);
                beforeGatewayHisTask(hisTaskKeys, (FlowNode) targetFlow,map,execution);
            }
            //排他网关
            else if(targetFlow instanceof ExclusiveGateway){
                beforeGatewayHisTask(hisTaskKeys, (FlowNode) targetFlow,map,execution);
            }
            else{
                if(hisTaskKeys.contains(targetFlow.getId())){
                    if( targetFlow instanceof UserTask){
                        map.put(targetFlow.getId(),targetFlow);
                        beforeGatewayHisTask(hisTaskKeys, (FlowNode) targetFlow,map,execution);
                    }
                }
            }
        }
    }


    //同一分支线上的历史节点(网关节点之后)，逆向查找
    private void afterGatewayHisTask(ArrayList<String> hisTaskKeys, FlowNode currentNode, Map<String, FlowElement> map,ExecutionEntity execution){
        List<SequenceFlow> inLines = currentNode.getIncomingFlows();
        for(SequenceFlow sequenceFlow : inLines ) {
            FlowElement sourceFlow = sequenceFlow.getSourceFlowElement();
            //嵌入式子流程
            if(sourceFlow instanceof SubProcess ){
                Map<String,FlowElement> subNodes = WorkflowUtil.getSubProcessRejectableTask(hisTaskKeys, (SubProcess) sourceFlow);
                map.putAll(subNodes);
                afterGatewayHisTask(hisTaskKeys, (FlowNode) sourceFlow,map,execution);
            }
            //调用活动
            else if(sourceFlow instanceof CallActivity){
                map.put(sourceFlow.getId(),sourceFlow);
                afterGatewayHisTask(hisTaskKeys, (FlowNode) sourceFlow,map,execution);
            }
            //排他网关
            else if(sourceFlow instanceof ExclusiveGateway){
                boolean exprResult = managementService.executeCommand(new EvaluateExpressionCmd(sequenceFlow,execution));
                if (exprResult) {
                    afterGatewayHisTask(hisTaskKeys, (FlowNode) sourceFlow,map,execution);
                }
            }
            //普通任务
            else{
                if(hisTaskKeys.contains(sourceFlow.getId())){
                    if( sourceFlow instanceof UserTask){
                        map.put(sourceFlow.getId(),sourceFlow);
                        afterGatewayHisTask(hisTaskKeys, (FlowNode) sourceFlow,map,execution);
                    }
                }
            }
        }
    }


    //查询父流程上可驳回的节点(暂设计为子流程只能驳回到父流程上发起子流程时的节点，且该节点只能是用户任务节点)
    private List<RejectTask.TaskNode> getParentRejectableNode(String subProcessInstanceId){

        ArrayList<RejectTask.TaskNode> list = new ArrayList();
        //子流程实例Execution
        ExecutionEntity instanceExecution =  (ExecutionEntity) runtimeService.createExecutionQuery().executionId(subProcessInstanceId).singleResult();
        //存在父流程
        if(instanceExecution.getSuperExecutionId()!=null){

            ExecutionEntity superExecution =  (ExecutionEntity) runtimeService.createExecutionQuery().executionId(instanceExecution.getSuperExecutionId()).singleResult();
            HistoricProcessInstance hisInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(subProcessInstanceId).singleResult();
            HistoricProcessInstance parentHisInstance = historyService.createHistoricProcessInstanceQuery().processInstanceId(hisInstance.getSuperProcessInstanceId()).singleResult();

            //查询流程配置是否允许子流程驳回到父流程
            ProcessDefinitionConfigModel definitionConfig = processDefinitionService.getProcessConfig(parentHisInstance.getProcessDefinitionId());
            if(definitionConfig.getRejectParentProcess()==null||!definitionConfig.getRejectParentProcess()){
                return list;
            }

            //父流程审批过的节点
            ArrayList<String> hisTaskKeys = new ArrayList<>();
            List<HistoricTaskInstance> hisTaskList = historyService.createHistoricTaskInstanceQuery()
                    .processInstanceId(parentHisInstance.getId()).finished()
                    .orderByHistoricTaskInstanceEndTime().desc()
                    .list();
            hisTaskList.forEach(historicTaskInstance -> {
                hisTaskKeys.add(historicTaskInstance.getTaskDefinitionKey());
            });

            //对应的父流程调用活动节点
            String parentActivityId = superExecution.getActivityId();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(parentHisInstance.getProcessDefinitionId());
            FlowNode parentNode = (FlowNode) bpmnModel.getFlowElement(parentActivityId);
            List<SequenceFlow> inLines = parentNode.getIncomingFlows();
            for(SequenceFlow sequenceFlow : inLines ) {
                FlowElement sourceFlow = sequenceFlow.getSourceFlowElement();
                if(hisTaskKeys.contains(sourceFlow.getId())){
                    if( sourceFlow instanceof UserTask){
                        RejectTask.TaskNode node = new RejectTask.TaskNode();
                        node.setFlowElementId(sourceFlow.getId());
                        node.setFlowElementName(sourceFlow.getName());
                        node.setFlowElementType(FlowElementType.USER_TASK);
                        node.setParentProcessInstanceId(parentHisInstance.getId());
                        node.setRejectPosition(RejectPosition.AFTER_GATEWAY);
                        list.add(node);
                    }
                }
            }
        }
        return list;
    }


    //获取任务节点类型(串行会签、并行会签、用户任务)
    private String getUserTaskType(UserTask userTask){
        String type;
        MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
        if(multiInstance!=null){
            if(multiInstance.isSequential()) type= FlowElementType.SEQUENTIAL_TASK;
            else type= FlowElementType.PARALLEL_TASK;
        }else{
            type= FlowElementType.USER_TASK;
        }
        return type;
    }



}
