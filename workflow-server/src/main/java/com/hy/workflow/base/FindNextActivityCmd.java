package com.hy.workflow.base;

import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.collections.CollectionUtils;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.condition.ConditionUtil;

import java.util.ArrayList;
import java.util.List;

public class FindNextActivityCmd implements Command<List<FlowNode>> {

    private final ExecutionEntity execution;
    private final BpmnModel bpmnModel;

    // 返回下一审批节点
    private List<FlowNode> activityList = new ArrayList<>();


    /**
     * 构造方法
     *
     * @param execution 当前执行实例
     * @param bpmnModel 当前执行实例的模型
     */
    public FindNextActivityCmd(ExecutionEntity execution, BpmnModel bpmnModel) {
        this.execution = execution;
        this.bpmnModel = bpmnModel;
    }


    @Override
    public List<FlowNode> execute(CommandContext commandContext) {
        FlowElement currentNode = bpmnModel.getFlowElement(execution.getActivityId());
        List<SequenceFlow> outgoingFlows = ((FlowNode) currentNode).getOutgoingFlows();
        if (CollectionUtils.isNotEmpty(outgoingFlows)) {
            this.findNextActivity(outgoingFlows, execution);
        }
        return activityList;
    }

    private void findNextActivity(List<SequenceFlow> outgoingFlows, ExecutionEntity execution) {
        for (SequenceFlow outgoingFlow : outgoingFlows) {
            FlowElement sourceFlow = outgoingFlow.getSourceFlowElement();
            FlowElement targetFlow = outgoingFlow.getTargetFlowElement();
            String expr =  outgoingFlow.getConditionExpression();
            //源节点为排他网关时 查找 分支条件为true的第一个节点
            if(sourceFlow instanceof ExclusiveGateway){
                if (expr!=null && ConditionUtil.hasTrueCondition(outgoingFlow, execution)) {
                    addActivityNode(targetFlow);
                    break;
                }
            }
            //源节点为包含网关时 查找 无分支条件 或者 有分支条件并且为true的节点
            else if(sourceFlow instanceof InclusiveGateway){
                if ( expr==null || (expr!=null && ConditionUtil.hasTrueCondition(outgoingFlow, execution)) ) {
                    addActivityNode(targetFlow);
                }
            }
            //源节点为普通任务节点或者并行网关时 不判断分支条件
            else{
                addActivityNode(targetFlow);
            }
        }
    }

    private void addActivityNode(FlowElement targetFlow){
        if (targetFlow instanceof UserTask) {
            activityList.add( (UserTask) targetFlow );
        }
        else if(targetFlow instanceof Gateway){  //网关继续向后查找
            Gateway exclusiveGateway = (Gateway)targetFlow;
            findNextActivity(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof SubProcess){  //子流程查找其第一个节点
            SubProcess subProcess = (SubProcess)targetFlow;
            UserTask userTask = WorkflowUtil.getSubProcessFirstTask(subProcess);
            activityList.add( userTask );
        }
        else if(targetFlow instanceof CallActivity){
            CallActivity callActivity = (CallActivity)targetFlow;
            activityList.add( callActivity );
        }
        else if(targetFlow instanceof EndEvent){
            EndEvent endEvent = (EndEvent)targetFlow;
            FlowElementsContainer container = targetFlow.getParentContainer();
            if(container instanceof SubProcess){
                findNextActivity(((SubProcess) container).getOutgoingFlows(), execution);
            }else{
                activityList.add( endEvent );
            }
        }

        /*else if(targetFlow instanceof ExclusiveGateway){
            ExclusiveGateway exclusiveGateway = (ExclusiveGateway)targetFlow;
            findNextActivity(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof ExclusiveGateway){
            ExclusiveGateway exclusiveGateway = (ExclusiveGateway)targetFlow;
            findNextActivity(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof ParallelGateway){
            ParallelGateway exclusiveGateway = (ParallelGateway)targetFlow;
            findNextActivity(exclusiveGateway.getOutgoingFlows(), execution);
        }
        else if(targetFlow instanceof InclusiveGateway){
            InclusiveGateway exclusiveGateway = (InclusiveGateway)targetFlow;
            findNextActivity(exclusiveGateway.getOutgoingFlows(), execution);
        }*/

    }


}