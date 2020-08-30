package com.hy.workflow.service;

import com.hy.workflow.base.FindNextActivityCmd;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveRequest;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.FlowElementModel;
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
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            taskService.complete(approveRequest.getTaskId(),approveRequest.getVariables());
        }
        //驳回
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
        List<FlowElementModel> subFlowList =  new ArrayList<>();

        Process process =  model.getMainProcess();
        FlowElement startNode = process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) ((StartEvent) startNode).getOutgoingFlows().get(0).getTargetFlowElement();
        List<SequenceFlow> outgoingFlows = firstNode.getOutgoingFlows();

        for (SequenceFlow outgoingFlow : outgoingFlows) {
            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();

            //用户任务（包含会签）
            if (targetFlowElement instanceof UserTask) {
                FlowElementModel flow = new FlowElementModel();
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
            //调用活动
            else if(targetFlowElement instanceof CallActivity){

                CallActivity callActivity = (CallActivity)targetFlowElement;
                FlowElementConfig callActivityConfig = flowElementConfigRepository.findByProcessDefinitionIdAndFlowElementId(processDefinitionId,callActivity.getId());
                String subProcessModel= callActivityConfig.getSubProcessModel();
                if(StringUtils.isBlank(subProcessModel)) throw new WorkflowException(callActivity.getName()+"未配置子流程：processDefinitionId:"+processDefinitionId);
                if(subProcessModel.endsWith(",")) subProcessModel = subProcessModel.substring(0,subProcessModel.length()-1);
                String[]  subModelArr = subProcessModel.split(",");

                for(String subModel : subModelArr ){
                    ProcessDefinition subProcessDefinition = repositoryService.createProcessDefinitionQuery().processDefinitionKey(subModel).latestVersion().singleResult();
                    if(subProcessDefinition==null) throw new WorkflowException("配置的子流程：modelId:"+subModel +"未部署");

                    //查找子流程第一个节点及该节点配置
                    Process subProcess =  repositoryService.getBpmnModel(subProcessDefinition.getId()).getMainProcess();
                    StartEvent subStartEvent = (StartEvent) subProcess.getInitialFlowElement();
                    FlowNode subFirstNode =(FlowNode) subStartEvent.getOutgoingFlows().get(0).getTargetFlowElement();
                    FlowElementConfig subFirstNodeConfig = flowElementConfigRepository.findByProcessDefinitionIdAndFlowElementId(subProcessDefinition.getId(),subFirstNode.getId());
                    FlowElementConfigModel configModel = EntityModelUtil.toFlowElementConfigMode(subFirstNodeConfig);
                    //查找子流程配置的部门
                    ProcessDefinitionConfig config = processDefinitionConfigRepository.findByProcessDefinitionId(subProcessDefinition.getId());
                    String depts = config.getDepartmentId();
                    if(StringUtils.isBlank(depts)){ //未配置子流程部门
                        FlowElementModel subFlow = new FlowElementModel();
                        subFlow.setId(subFirstNode.getId());
                        subFlow.setName(subFirstNode.getName());
                        subFlow.setFlowElementType(FlowElementType.USER_TASK);
                        subFlow.setParentId( callActivity.getId() );
                        subFlow.setParentName( callActivity.getName() );
                        subFlow.setParentType( FlowElementType.CALL_ACTIVITY);
                        subFlow.setModelKey(subModel);
                        subFlow.setConfig(configModel);
                        subFlowList.add(subFlow);
                    }
                    else{
                        String[] arr = depts.split(",");
                        for(String d : arr ){
                            FlowElementModel subFlow = new FlowElementModel();
                            subFlow.setId(subFirstNode.getId());
                            subFlow.setName(subFirstNode.getName());
                            subFlow.setFlowElementType(FlowElementType.USER_TASK);
                            subFlow.setParentId( callActivity.getId() );
                            subFlow.setParentType( FlowElementType.CALL_ACTIVITY);
                            subFlow.setParentName( callActivity.getName() );
                            subFlow.setDepartmentId(d);
                            subFlow.setModelKey(subModel);
                            subFlow.setConfig(configModel);
                            subFlowList.add(subFlow);
                        }
                    }
                }
            }
            //子流程
            else if(targetFlowElement instanceof SubProcess){
                SubProcess subProcess = (SubProcess)targetFlowElement;
                UserTask userTask = WorkflowUtil.getSubProcessFirstTask(subProcess,true);
                if(userTask!=null){
                    FlowElementModel flow = new FlowElementModel();
                    flow.setId(userTask.getId());
                    flow.setName(userTask.getName());
                    flow.setFlowElementType(FlowElementType.USER_TASK);
                    FlowElementsContainer container = userTask.getParentContainer();
                    flow.setParentId( ((SubProcess)container).getId() );
                    flow.setParentName( ((SubProcess) container).getName() );
                    flow.setParentType( FlowElementType.SUB_PROCESS);
                    flowList.add(flow);
                    flowIdList.add(flow.getId());
                }
            }
            //其他类型节点
            else{
                throw new WorkflowException("第二节点只能为用户任务节点或者子流程！");
            }
        }

        //设置节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);

        flowList.addAll(subFlowList);

        return flowList;
    }


    /**
     * 根据当前任务ID查询下一个审批节点
     *
     * @author  zhaoyao
     * @param taskId 任务ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getNextUserTask(String taskId) {

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List<Activity> activityList = managementService.executeCommand(new FindNextActivityCmd(execution, bpmnModel));
        List<FlowElementModel> flowList = new ArrayList<>();
        List<String> flowIdList =  new ArrayList<>();

        activityList.forEach( activity -> {

            FlowElementModel model =  new FlowElementModel();
            model.setId(activity.getId());
            model.setName(activity.getName());
            if(activity instanceof UserTask){
                MultiInstanceLoopCharacteristics multiInstance = activity.getLoopCharacteristics();
                if(multiInstance!=null){
                    if(multiInstance.isSequential()) model.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                    else model.setFlowElementType(FlowElementType.PARALLEL_TASK);
                }else{
                    model.setFlowElementType(FlowElementType.USER_TASK);
                }
                FlowElementsContainer container = activity.getParentContainer();
                if(container instanceof SubProcess){
                    model.setParentId( ((SubProcess)container).getId() );
                    model.setParentName( ((SubProcess) container).getName() );
                }
            }else if(activity instanceof CallActivity){
                model.setFlowElementType(FlowElementType.CALL_ACTIVITY);
            }else {
                throw new WorkflowException("不支持的节点类型：processDefinitionId="+task.getProcessDefinitionId()+"  flowElementId="+task.getTaskDefinitionKey());
            }

            flowList.add(model);
            flowIdList.add(activity.getId());

        });
        //查询并填充节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);

        return flowList;

    }



}
