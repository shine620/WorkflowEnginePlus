package com.hy.workflow.util;

import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveInfo;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowUtil {


    /**
     * 获取子流程中的第一个审批节点
     *
     * @author  zhaoyao
     * @param subProcess 子流程对象
     * @param validate 是否正确性校验
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


    /**
     * 设置流程发起或审批时需要的流程变量信息
     *
     * @author  zhaoyao
     * @param variables 接受返回变量的集合
     * @param approveInfo 审批请求数据
     * @return List<FlowElementModel>
     */
    public static Map<String,Object>  setNextTaskInfoVariables(Map<String,Object> variables, ApproveInfo approveInfo ){

        if(approveInfo.getNextTaskList()==null) return variables;
        Map<String,List<String>>  modelkeyMap = new HashMap();              //调用活动多实例的子流程模型ID
        List<Map<String,Object>> callActivityMultiInfo = new ArrayList<>();  //设置调用活动多实例的处理人信息

        approveInfo.getNextTaskList().forEach(nextTask ->{

            /*用户任务*/
            if(FlowElementType.USER_TASK.equals(nextTask.getFlowElementType())){
                //调用活动中的用户任务节点（流程模型第一个节点不允许为会签）
                if( nextTask.getParentFlowElementType()!=null && FlowElementType.CALL_ACTIVITY.equals(nextTask.getParentFlowElementType()) ){
                    List<String>  modelKeyList; //设置调用活动多实例发起时需要的模型变量
                    if(modelkeyMap.get(nextTask.getParentFlowElementId())==null) modelKeyList = new ArrayList<>();
                    else modelKeyList = modelkeyMap.get(nextTask.getParentFlowElementId());
                    modelKeyList.add(nextTask.getModelKey());
                    modelkeyMap.put(nextTask.getParentFlowElementId(),modelKeyList);
                    //设置调用活动多实例生成任务时需要的审批人相关信息
                    Map<String,Object> map = new HashMap<>();
                    map.put("flowElementId",nextTask.getFlowElementId());
                    map.put("parentFlowElementId",nextTask.getParentFlowElementId());
                    map.put("modelKey",nextTask.getModelKey());
                    map.put("assignee",nextTask.getAssignee());
                    map.put("candidateUser",nextTask.getCandidateUser());
                    map.put("candidateGroup",nextTask.getCandidateGroup());
                    callActivityMultiInfo.add(map);
                }
                //正常用户任务节点
                else{
                    Map<String,Object> taskUserMap = new HashMap<>();
                    taskUserMap.put("assignee",nextTask.getAssignee());
                    taskUserMap.put("candidateUser",nextTask.getCandidateUser());
                    taskUserMap.put("candidateGroup",nextTask.getCandidateGroup());
                    taskUserMap.put("flowElementType",nextTask.getFlowElementType());
                    variables.put( nextTask.getFlowElementId(), taskUserMap );
                }
            }
            /*会签*/
            else if(FlowElementType.PARALLEL_TASK.equals(nextTask.getFlowElementType()) || FlowElementType.SEQUENTIAL_TASK.equals(nextTask.getFlowElementType())){
                Map<String,Object> taskUserMap = new HashMap<>();
                if(nextTask.getCandidateUser()!=null && nextTask.getCandidateUser().size()>0)
                    taskUserMap.put("assigneeList",nextTask.getCandidateUser());
                else if(StringUtils.isNotBlank(nextTask.getAssignee()))
                    taskUserMap.put("assigneeList",nextTask.getAssignee());
                taskUserMap.put("flowElementType",nextTask.getFlowElementType());
                variables.put( nextTask.getFlowElementId(), taskUserMap );
            }
            /*结束节点*/
            else if(FlowElementType.END_EVENT.equals(nextTask.getFlowElementType())){
                //无需设置变量信息
            }

        });

        variables.put( "callActivityList", callActivityMultiInfo );
        variables.put( "callActivityModelKeyMap", modelkeyMap );

        return variables;

    }


}
