package com.hy.workflow.base;

import org.apache.commons.collections.CollectionUtils;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.condition.ConditionUtil;

import java.util.ArrayList;
import java.util.List;

public class FindNextUserTaskNodeCmd implements Command<List<UserTask>> {

    private final ExecutionEntity execution;
    private final BpmnModel bpmnModel;

    // 返回下一用户节点
    private List<UserTask> taskList = new ArrayList<>();


    /**
     * 构造方法
     *
     * @param execution 当前执行实例
     * @param bpmnModel 当前执行实例的模型
     */
    public FindNextUserTaskNodeCmd(ExecutionEntity execution, BpmnModel bpmnModel) {
        this.execution = execution;
        this.bpmnModel = bpmnModel;
    }


    @Override
    public List<UserTask> execute(CommandContext commandContext) {
        FlowElement currentNode = bpmnModel.getFlowElement(execution.getActivityId());
        List<SequenceFlow> outgoingFlows = ((FlowNode) currentNode).getOutgoingFlows();
        if (CollectionUtils.isNotEmpty(outgoingFlows)) {
            this.findNextUserTaskNode(outgoingFlows, execution);
        }
        return taskList;
    }

    private void findNextUserTaskNode(List<SequenceFlow> outgoingFlows, ExecutionEntity execution) {
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            FlowElement sourceFlow = outgoingFlow.getSourceFlowElement();
            FlowElement targetFlow = outgoingFlow.getTargetFlowElement();
            String expr =  outgoingFlow.getConditionExpression();
            //源节点为排他网关时 查找 分支条件为true的第一个节点
            if(sourceFlow instanceof ExclusiveGateway){
                if (expr!=null && ConditionUtil.hasTrueCondition(outgoingFlow, execution)) {
                    addUserTask(targetFlow);
                    break;
                }
            }
            //源节点为包含网关时 查找 无分支条件或者有分支条件并且为true的节点
            else if(sourceFlow instanceof InclusiveGateway){
                if ( expr==null || (expr!=null && ConditionUtil.hasTrueCondition(outgoingFlow, execution)) ) {
                    addUserTask(targetFlow);
                }
            }
            //源节点为并行网关或者任务节点时 不判断分支条件
            else{
                addUserTask(targetFlow);
            }
        }
    }

    private void addUserTask(FlowElement targetFlow){
        if (targetFlow instanceof UserTask) {
            taskList.add( (UserTask) targetFlow );
        }
        else if(targetFlow instanceof ExclusiveGateway){
            ExclusiveGateway exclusiveGateway = (ExclusiveGateway)targetFlow;
            findNextUserTaskNode(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof ExclusiveGateway){
            ExclusiveGateway exclusiveGateway = (ExclusiveGateway)targetFlow;
            findNextUserTaskNode(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof ParallelGateway){
            ParallelGateway exclusiveGateway = (ParallelGateway)targetFlow;
            findNextUserTaskNode(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof InclusiveGateway){
            InclusiveGateway exclusiveGateway = (InclusiveGateway)targetFlow;
            findNextUserTaskNode(exclusiveGateway.getOutgoingFlows(), execution);
        }
    }


}