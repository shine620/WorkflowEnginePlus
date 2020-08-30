package com.hy.workflow.util;

import com.hy.workflow.base.WorkflowException;
import org.flowable.bpmn.model.*;

import java.util.List;

public class WorkflowUtil {


    /**
     * 获取子流程中的第一个审批节点
     *
     * @author  zhaoyao
     * @param subProcess 子流程对象
     * @validate validate 是否正确性校验
     * @return List<FlowElementModel>
     */
    public static UserTask getSubProcessFirstTask(SubProcess subProcess, boolean validate){
        StartEvent startEvent = null;
        UserTask userTask = null;
        for( FlowElement flowElement : subProcess.getFlowElements()){
            if(flowElement instanceof StartEvent){
                startEvent = (StartEvent) flowElement;
                List<SequenceFlow> sf = startEvent.getOutgoingFlows();
                if(validate && sf.size()>1) throw new WorkflowException("子流程开始节点存在两条输出线！");
                FlowElement firstNode = sf.get(0).getTargetFlowElement();
                if(firstNode instanceof UserTask) userTask = (UserTask) firstNode;
                break;
            }
        }
        if( validate && startEvent == null ) throw new WorkflowException("子流程中无开始节点 flowElementId："+subProcess.getId());
        if( validate && userTask == null ){
            throw new WorkflowException("子流程第一个审批节点不是用户任务节点 flowElementId："+subProcess.getId());
        }
        return userTask;
    }



}
