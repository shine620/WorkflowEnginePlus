package com.hy.workflow.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveInfo;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.TaskService;
import org.flowable.task.api.DelegationState;
import org.flowable.task.api.Task;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class WorkflowUtil {

    private static final Logger logger = LoggerFactory.getLogger(WorkflowUtil.class);


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
     * 获取子流程内部的可驳回节点
     *
     * @author  zhaoyao
     * @param hisTaskKeys 审批过的历史节点
     * @param subProcess 子流程对象
     * @return Map<String,FlowElement>
     */
    public static Map<String,FlowElement> getSubProcessRejectableTask(ArrayList<String> hisTaskKeys, SubProcess subProcess){
        Map<String,FlowElement> map = new HashMap<>();
        for( FlowElement flowElement : subProcess.getFlowElements()){
            if(flowElement instanceof UserTask){
                if(hisTaskKeys.contains(flowElement.getId())){
                    map.put(flowElement.getId(),flowElement);
                }
            }else if(flowElement instanceof ParallelGateway||flowElement instanceof InclusiveGateway){
                //嵌入式子流程存在并行或者包含网关节点时直接返回空，不允许驳回
                return new HashMap<>();
            }
        }
        return map;
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
        Map<String,List<String>>  callActivityModelKeyMap = new HashMap();    //调用活动多实例时发起的子流程模型ID(决定子流程数量)
        List<Map<String,Object>> callActivityList = new ArrayList<>();                 //调用活动多实例时每个子流程的处理人信息

        approveInfo.getNextTaskList().forEach(nextTask ->{

            if( StringUtils.isBlank(nextTask.getAssignee()) && (nextTask.getCandidateUser()==null||nextTask.getCandidateUser().size()==0)
                    && !nextTask.getFlowElementType().equals(FlowElementType.END_EVENT)){
                throw new WorkflowException("审批人和候选人必须有一个不为空");
            }

            /*用户任务*/
            if(FlowElementType.USER_TASK.equals(nextTask.getFlowElementType())){
                //调用活动中的用户任务节点（流程模型第一个节点不允许为会签）
                if( nextTask.getParentFlowElementType()!=null && FlowElementType.CALL_ACTIVITY.equals(nextTask.getParentFlowElementType()) ){
                    List<String>  modelKeyList; //设置调用活动多实例发起时需要的模型变量
                    if(callActivityModelKeyMap.get(nextTask.getParentFlowElementId())==null) modelKeyList = new ArrayList<>();
                    else modelKeyList = callActivityModelKeyMap.get(nextTask.getParentFlowElementId());
                    modelKeyList.add(nextTask.getModelKey());
                    callActivityModelKeyMap.put(nextTask.getParentFlowElementId(),modelKeyList); /** modelkeyMap格式： { "FaWuHuiQian":["SubModelA","SubModelB"], "CaiWuHuiQian":["SubModelC","SubModelD"] } */
                    //设置调用活动多实例生成任务时需要的审批人相关信息
                    Map<String,Object> map = new HashMap<>();
                    map.put("flowElementId",nextTask.getFlowElementId());
                    map.put("parentFlowElementId",nextTask.getParentFlowElementId());
                    map.put("modelKey",nextTask.getModelKey());
                    map.put("assignee",nextTask.getAssignee());
                    map.put("candidateUser",nextTask.getCandidateUser());
                    map.put("candidateGroup",nextTask.getCandidateGroup());
                    map.put("suProcessDepartmentId",nextTask.getSuProcessDepartmentId());
                    callActivityList.add(map);
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
                    taskUserMap.put("assigneeList", Lists.newArrayList(nextTask.getAssignee()));
                taskUserMap.put("flowElementType",nextTask.getFlowElementType());
                variables.put( nextTask.getFlowElementId(), taskUserMap );
            }
            /*结束节点*/
            else if(FlowElementType.END_EVENT.equals(nextTask.getFlowElementType())){
                //无需设置变量信息
            }

        });

        if(callActivityList.size()>0) variables.put( "callActivityList", callActivityList );
        if(callActivityModelKeyMap.size()>0) variables.put( "callActivityModelKeyMap", callActivityModelKeyMap );

        return variables;

    }


    /**
     * 选择下一节点时的任务审批
     *
     * 审批时要走向用户选择的下一节点分支时，需要拆除原来未选择的流程分支线，
     * 会存在同一流程模型实例审批时线程安全问题，因此代码同步保证流向的正确性
     *
     * @author  zhaoyao
     * @param selectOutNode 已选择的下一节点ID
     * @param currentNode 当前审批的节点对象
     * @param taskService TaskService
     * @param task 当前审批的任务对象
     * @param variables 流程变量
     */
    public static void completeTaskBySelectNode(List<String> selectOutNode, FlowNode currentNode, TaskService taskService, Task task, Map variables){
        synchronized (WorkflowUtil.class) { //TODO 应该使用流程定义对象
            List<SequenceFlow> outLines = currentNode.getOutgoingFlows();
            //剪断当前节点未选择的下一分支流向
            List<SequenceFlow> removedNodes = new ArrayList<>();
            Iterator<SequenceFlow> it = outLines.listIterator();
            while (it.hasNext() && selectOutNode.size() > 0) {
                SequenceFlow sf = it.next();
                FlowElement target = sf.getTargetFlowElement();
                if (target instanceof Gateway) {
                    List<FlowElement> containsNode = new ArrayList<>();
                    findContainsNode(target,selectOutNode,containsNode);
                    if(containsNode.isEmpty()){
                        removedNodes.add(sf);
                        it.remove();
                    }
                }
                else{
                    if (!selectOutNode.contains(target.getId())) {
                        removedNodes.add(sf);
                        it.remove();
                    }
                }
            }
            //委托的任务要先处理委托
            if(DelegationState.PENDING.equals(task.getDelegationState())){
                taskService.resolveTask(task.getId());
            }
            //审批任务
            taskService.complete(task.getId(), variables);
            //审批完成后还原原来的分支流向
            outLines.addAll(removedNodes);
        }
    }


    //查找在网关节点之后且包含在已选节点中的节点
    private static void findContainsNode(FlowElement flowElement,List<String> selectOutNode,List<FlowElement> containsNode){
        List<SequenceFlow> outLines = ((FlowNode)flowElement).getOutgoingFlows();
        Iterator<SequenceFlow> it = outLines.listIterator();
        while (it.hasNext()) {
            FlowElement target = it.next().getTargetFlowElement();
            if (target instanceof Gateway) {
                findContainsNode(flowElement,selectOutNode,containsNode);
            }
            else{
                if (selectOutNode.contains(target.getId())) {
                    containsNode.add(target);
                }
            }
        }
    }


    /**
     * 获取任务节点类型(串行会签、并行会签、用户任务)
     *
     * @author  zhaoyao
     * @param userTask 任务节点对象
     */
    public static String getUserTaskType(UserTask userTask){
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


    /**
     * 将JSON数据转换成BpmnModel对象
     *
     * @author  zhaoyao
     * @param jsonByte json数据
     */
    public static BpmnModel jsonConvertBnpmModel(byte[] jsonByte) {
        ObjectMapper objectMapper = new ObjectMapper();
        BpmnJsonConverter bpmnJsonConverter = new BpmnJsonConverter();
        ObjectNode editorJsonNode = null;
        try {
            editorJsonNode = (ObjectNode) objectMapper.readTree(jsonByte);
        } catch (IOException e) {
            logger.error("JSON数据读取异常");
        }
        return bpmnJsonConverter.convertToBpmnModel(editorJsonNode);
    }


    /**
     * 从BpmnModel对象中获取xml数据
     *
     * @author  zhaoyao
     * @param bpmnModel BpmnModel对象
     * @param verifyKey 验证模型Key是否数字开头，并对其修正(流程模型不能以数字开头)
     */
    public static byte[] getBpmnXmlByte(BpmnModel bpmnModel,Boolean verifyKey) {
        if(verifyKey){
            for (Process process : bpmnModel.getProcesses()) {
                if (StringUtils.isNotEmpty(process.getId())) {
                    char firstCharacter = process.getId().charAt(0);
                    // 流程模型key不允许将数字作为第一个字符
                    if (Character.isDigit(firstCharacter)) {
                        process.setId("A" + process.getId());
                    }
                }
            }
        }
        BpmnXMLConverter bpmnXMLConverter = new BpmnXMLConverter();
        byte[] xmlBytes = bpmnXMLConverter.convertToXML(bpmnModel);
        return xmlBytes;
    }


    /**
     * 验证BpmnModel模型数据
     *
     * @author  zhaoyao
     * @param bpmnModel BpmnModel对象
     */
    public static void validateBpmnModel(BpmnModel bpmnModel) {
        if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
            throw new BadRequestException("未找到模型数据信息");
        }
    }



}
