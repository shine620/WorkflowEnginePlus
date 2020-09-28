package com.hy.workflow.service;

import com.alibaba.fastjson.JSONArray;
import com.hy.workflow.base.SpringContextUtil;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.MultiInstanceRecord;
import com.hy.workflow.entity.TaskRecord;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.TaskRecordRepository;
import com.hy.workflow.util.WorkflowUtil;
import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.impl.FlowableActivityCancelledEventImpl;
import org.flowable.engine.delegate.event.impl.FlowableMultiInstanceActivityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntity;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.*;


/* *
    ========================== 事件监听器 ======================
    流程发起、流程结束、任务结束：FlowableEntityEventImpl：org.flowable.engine.delegate.event.impl.FlowableEntityEventImp
    任务生成：FlowableEntityEventImpl：org.flowable.common.engine.impl.event.FlowableEntityEventImp
*/

@Component("processListener")
public class ProcessListener extends AbstractFlowableEngineEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProcessListener.class);

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;


    @Override
    public void onEvent(FlowableEvent event) {

        FlowableEventType eventType = event.getType();
        FlowableEngineEntityEvent flowEvent =  (event instanceof  FlowableEngineEntityEvent) ? (FlowableEngineEntityEvent)event : null;

        //流程开始
        if(FlowableEngineEventType.PROCESS_STARTED.equals(eventType)){
            processStarted( (ExecutionEntityImpl)flowEvent.getEntity() );
        }
        //流程结束
        else if(FlowableEngineEventType.PROCESS_COMPLETED.equals(eventType)){
            processCompleted( (ExecutionEntityImpl)flowEvent.getEntity() );
        }
        //流程结束（与PROCESS_COMPLETED相同，但包含略有不同的数据，例如结束时间，持续时间等）
        // 注意：必须启用历史记录(最低活动级别)才能接收此事件
        else if(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED.equals(eventType)){
            HistoricProcessInstanceEntityImpl execution = (HistoricProcessInstanceEntityImpl)flowEvent.getEntity();
            historicProcessInstanceEnded(execution);
        }
        //任务生成
        else if(FlowableEngineEventType.TASK_CREATED.equals(eventType)){
            taskCreated( (TaskEntityImpl)flowEvent.getEntity() );
        }
        //多实例开始(设置会签处理人)
        else if(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED.equals(eventType)){
            FlowableMultiInstanceActivityEventImpl multiIEvent= (FlowableMultiInstanceActivityEventImpl)event;
            multiInstanceActivityStarted(multiIEvent);
        }
        //任务完成
        else if(FlowableEngineEventType.TASK_COMPLETED.equals(eventType)){
            taskCompleted( (TaskEntityImpl)flowEvent.getEntity() );
        }
        //任务签收或设置处理人
        else if(FlowableEngineEventType.TASK_ASSIGNED.equals(eventType)){
            taskAssigned((TaskEntityImpl)flowEvent.getEntity());
        }
        //节点取消(删除流程、驳回时调用)
        else if(FlowableEngineEventType.ACTIVITY_CANCELLED.equals(eventType)){
            FlowableActivityCancelledEventImpl activityCancelled = (FlowableActivityCancelledEventImpl) event;
            activityCancelled(activityCancelled);
        }
        //任务所属人(委托或者转办时)
        else if(FlowableEngineEventType.TASK_OWNER_CHANGED.equals(eventType)){
            taskOwnerChanged((TaskEntityImpl)flowEvent.getEntity());
        }
    }


    private void taskCreated(TaskEntityImpl taskEntity){
        logger.info("任务生成监听事件执行 processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        //如果是并行发起其他任务和调用活动子流程，子流程中的第一个审批节点ID和父流程中并行发起的其他任务节点ID不能相同
        Map taskKeyVariable = taskEntity.getVariable(taskEntity.getTaskDefinitionKey(),Map.class);
        List<Map<String,Object>> callActivityList = taskEntity.getVariable("callActivityList",List.class);
        TaskRecord taskRecord = new TaskRecord();
        //通过流程变量设置任务处理人
         if(taskKeyVariable!=null){  //普通任务节点(通过任务类型排除会签)
            if(FlowElementType.USER_TASK.equals(taskKeyVariable.get("flowElementType"))){
                String assignee = (String)taskKeyVariable.get("assignee");
                List<String> candidateUser = (List)taskKeyVariable.get("candidateUser");
                List<String> candidateGroup = (List)taskKeyVariable.get("candidateGroup");
                //if(assignee!=null) taskEntity.setAssignee(assignee); 此种方式如果是驳回后重新提交的节点，一般记录中会缺失审批人信息
                TaskService taskService = SpringContextUtil.getBeanByClass(TaskService.class);
                if(assignee!=null) taskService.setAssignee(taskEntity.getId(),assignee);
                if(candidateUser!=null) taskEntity.addCandidateUsers(candidateUser);
                if(candidateGroup!=null) taskEntity.addCandidateGroups(candidateGroup);
                //记录处理人
                taskRecord.setAssignee(assignee);
                taskRecord.setCandidateUser(StringUtils.join(candidateUser,","));
                taskRecord.setCandidateGroup(StringUtils.join(candidateGroup,","));
                //任务创建后清空该变量，以免影响驳回操作
                taskEntity.removeVariable(taskEntity.getTaskDefinitionKey());
            }
        }
         //调用活动多实例生成的任务节点
        else if (callActivityList!=null && callActivityList.size()>0 ){
            //通过 任务Key、父节点ID、子流程模型Key来设置子流程接收人
            Iterator<Map<String,Object>> it = callActivityList.listIterator();
            while (it.hasNext()){
                Map<String,Object> map = it.next();
                if (map.get("flowElementId").equals(taskEntity.getTaskDefinitionKey())) {
                    RuntimeService runtimeService =SpringContextUtil.getBeanByClass(RuntimeService.class);
                    ProcessInstance currentInstance = runtimeService.createProcessInstanceQuery().processInstanceId(taskEntity.getProcessInstanceId()).singleResult();
                    String modelKey = currentInstance.getProcessDefinitionKey();
                    if (map.get("modelKey").equals(modelKey)) {
                        ExecutionEntityImpl superExecution = ( (ExecutionEntityImpl)currentInstance ).getSuperExecution();
                        String parentActivityId = superExecution!=null?superExecution.getActivityId():null;
                        if (map.get("parentFlowElementId").equals(parentActivityId)) {
                            String assignee = (String) map.get("assignee");
                            List<String> candidateGroup = (List) map.get("candidateGroup");
                            List<String> candidateUser = (List) map.get("candidateUser");
                            //if (assignee != null) taskEntity.setAssignee(assignee);
                            TaskService taskService = SpringContextUtil.getBeanByClass(TaskService.class);
                            if(assignee!=null) taskService.setAssignee(taskEntity.getId(),assignee);
                            if (candidateGroup != null) taskEntity.addCandidateGroups(candidateGroup);
                            if (candidateUser != null) taskEntity.addCandidateUsers(candidateUser);
                            //记录处理人
                            taskRecord.setAssignee(assignee);
                            taskRecord.setCandidateUser(StringUtils.join(candidateUser,","));
                            taskRecord.setCandidateGroup(StringUtils.join(candidateGroup,","));
                            taskRecord.setSuProcessDepartmentId((String)map.get("suProcessDepartmentId"));
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
        //生成任务记录
        saveTaskRecord(taskRecord,taskEntity);
    }


    private void taskCompleted(TaskEntityImpl taskEntity){
        logger.info("任务完成监听事件执行 processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
        TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
        if(taskRecord!=null){ //正常情况下任务记录表中的结束时间可能会与流程历史任务表结束时间相差1秒左右
            taskRecord.setEndTime(new Date());
            entityManager.merge(taskRecord);
        }
    }


    private void taskOwnerChanged(TaskEntityImpl taskEntity){
        logger.info("任务所属人变更 processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
        TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
        if(taskRecord!=null){
            taskRecord.setOwner(taskEntity.getOwner());
            entityManager.merge(taskRecord);
        }
    }


    private void activityCancelled(FlowableActivityCancelledEventImpl activityCancelled){
        if(activityCancelled.getCause() instanceof UserTask){
            TaskRecord taskRecord = taskRecordRepository.findByExecutionIdAndTaskDefinitionKey(activityCancelled.getExecutionId(),activityCancelled.getActivityId());
            if(taskRecord!=null){
                taskRecord.setEndTime(new Date());
                taskRecord.setCancelled(true);
                taskRecordRepository.save(taskRecord);
            }
        }else{
            logger.info("任务取消{}，processInstanceId={}，executionId={}",activityCancelled.getClass(),activityCancelled.getProcessInstanceId(),activityCancelled.getExecutionId());
        }
    }


    private void multiInstanceActivityStarted(FlowableMultiInstanceActivityEventImpl multiIEvent){
        logger.info("多实例开始 processInstanceId:{}  activityId:{}",multiIEvent.getProcessInstanceId(),multiIEvent.getActivityId());
        DelegateExecution execution = multiIEvent.getExecution();
        MultiInstanceRecord record = new MultiInstanceRecord();
        //调用活动多实例
        if(FlowElementType.CALL_ACTIVITY.equals(multiIEvent.getActivityType())){
            Map callActivityModelIdMap = execution.getVariable("callActivityModelKeyMap",Map.class);
            if(callActivityModelIdMap!=null){
                List modelIdList = (List) callActivityModelIdMap.get(multiIEvent.getActivityId());
                if(modelIdList!=null&&modelIdList.size()>0){
                    execution.setVariable("subProcessDefinitionKeyList",modelIdList);
                    //记录调用活动子流程
                    record.setSubProcessDefinitionKeyList( JSONArray.toJSON(modelIdList).toString() );
                    //移除发起调用活动时所需变量
                    callActivityModelIdMap.remove(multiIEvent.getActivityId());
                }
            }
        }
        //普通任务多实例
        else{
            Map userVariable = execution.getVariable(multiIEvent.getActivityId(),Map.class);
            if(userVariable!=null&&userVariable.size()>0){
                List<String> assigneeList = (List)userVariable.get("assigneeList");
                execution.setVariable("assigneeList",assigneeList);
                //记录会签人信息
                if( assigneeList!=null && assigneeList.size()>0 ) record.setAssigneeList( JSONArray.toJSON(assigneeList).toString() );
                //设置会签处理人后清除该任务节点变量
                execution.removeVariable(multiIEvent.getActivityId());
            }
        }
        //生成会签记录
        record.setProcessInstanceId(multiIEvent.getProcessInstanceId());
        record.setProcessDefinitionId(multiIEvent.getProcessDefinitionId());
        record.setActivityId(multiIEvent.getActivityId());
        record.setActivityName(multiIEvent.getActivityName());
        record.setActivityType(multiIEvent.getActivityType());
        List<Map<String,Object>> callActivityList = execution.getVariable("callActivityList",List.class);
        if( callActivityList!=null && callActivityList.size()>0 )
            record.setCallActivityList( JSONArray.toJSON(callActivityList).toString() );
        record.setCreateTime(new Date());
        EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
        entityManager.merge(record);
    }


    private void processStarted(ExecutionEntityImpl execution){
        logger.info("流程发起监听事件执行 processInstanceId:"+execution.getProcessInstanceId());
        ExecutionEntityImpl subProcessInstance = execution.getParent();
        if(subProcessInstance.getSuperExecution()!=null){ //子流程设置发起人和标题
            ExecutionEntity rootProcessInstance = execution.getRootProcessInstance();
            String subProcessName = rootProcessInstance.getName()+"-"+subProcessInstance.getProcessDefinitionName();
            subProcessInstance.setStartUserId(rootProcessInstance.getStartUserId());
            subProcessInstance.setName(subProcessName);
            execution.setStartUserId(rootProcessInstance.getStartUserId());
            execution.setName(subProcessName);
            RuntimeService runtimeService =SpringContextUtil.getBeanByClass(RuntimeService.class);
            runtimeService.setProcessInstanceName(subProcessInstance.getId(),subProcessName);
        }
        //TODO  此处应回调业务接口回写数据状态
    }


    private void processCompleted(ExecutionEntityImpl execution){
        logger.info("流程结束监听事件执行 processInstanceId: "+execution.getProcessInstanceId());
        //TODO  此处应回调业务接口回写数据状态
    }


    private void historicProcessInstanceEnded(HistoricProcessInstanceEntityImpl execution){
        logger.info("历史实例结束监听事件执行 processInstanceId:"+execution.getProcessInstanceId());
        Optional<BusinessProcess> op = businessProcessRepository.findById(execution.getProcessInstanceId());
        if(op.isPresent()){
            BusinessProcess bp = op.get();
            bp.setEnded(true);
            bp.setEndTime(execution.getEndTime());
            businessProcessRepository.save(bp);
        }
    }


    //签收或设置处理人
    private void taskAssigned(TaskEntityImpl taskEntity ){
        Optional<TaskRecord> optional = taskRecordRepository.findById( taskEntity.getId() );
        if(optional.isPresent()){
            TaskRecord taskRecord = optional.get();
            taskRecord.setAssignee(taskEntity.getAssignee());
            taskRecord.setClaimTime(taskEntity.getClaimTime());
            taskRecordRepository.save(taskRecord);
        }
    }


    //刪除任务记录
    private void deleteTaskRecord(FlowableEngineEntityEvent flowEvent){
        if(flowEvent.getEntity() instanceof HistoricTaskInstanceEntityImpl){
            HistoricTaskInstanceEntityImpl taskEntity = (HistoricTaskInstanceEntityImpl) flowEvent.getEntity();
            if(taskEntity.isDeleted()){
                logger.info("任务删除：{},taskId={}，processInstanceId={}",taskEntity.getName(),taskEntity.getId(),taskEntity.getProcessInstanceId());
                /*EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
                TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
                if(taskRecord!=null){
                    entityManager.remove(taskRecord);
                }*/
                taskRecordRepository.deleteById(taskEntity.getId());
            }
        }
    }


    //生成任务记录
    private void saveTaskRecord(TaskRecord taskRecord, TaskEntity taskEntity){
        taskRecord.setTaskId(taskEntity.getId());
        taskRecord.setTaskName(taskEntity.getName());
        taskRecord.setTaskDefinitionKey(taskEntity.getTaskDefinitionKey());
        taskRecord.setAssignee(taskEntity.getAssignee());
        taskRecord.setOwner(taskEntity.getOwner());
        taskRecord.setCreateTime(taskEntity.getCreateTime());
        taskRecord.setClaimTime(taskEntity.getClaimTime());
        RuntimeService runtimeService =SpringContextUtil.getBeanByClass(RuntimeService.class);
        ProcessInstance currentInstance = runtimeService.createProcessInstanceQuery().processInstanceId(taskEntity.getProcessInstanceId()).singleResult();
        taskRecord.setProcessInstanceId(currentInstance.getId());
        taskRecord.setProcessInstanceName(currentInstance.getName());
        taskRecord.setBusinessType(currentInstance.getBusinessKey().split(";")[0]);
        taskRecord.setBusinessId(currentInstance.getBusinessKey().split(";")[1]);
        taskRecord.setBusinessKey(currentInstance.getBusinessKey());
        ExecutionEntity taskExecution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(taskEntity.getExecutionId()).singleResult();
        taskRecord.setExecutionId(taskExecution.getId());
        taskRecord.setParentExecutionId(taskExecution.getParentId());
        taskRecord.setProcessDefinitionId(taskEntity.getProcessDefinitionId());
        FlowElement flowElement = taskExecution.getCurrentFlowElement();
        if(flowElement instanceof UserTask){
            taskRecord.setTaskType(WorkflowUtil.getUserTaskType((UserTask) flowElement));
        }
        taskRecordRepository.save(taskRecord);
    }


}





/*
 事件监听器可以在流程引擎配置中注册，也可以配置在xml中
<process id="YGJDLC" name="一个简单流程" isExecutable="true">
    <extensionElements>
        <flowable:eventListener events="PROCESS_STARTED" delegateExpression="${processListener}"/>
        <flowable:eventListener events="PROCESS_COMPLETED" delegateExpression="${processListener}"/>
    </extensionElements>
</process>
*/


/* *
    ======================执行监听器（监听流程实例、流程线的start、end、take事件） ======================
*/

/*
<process id="YGJDLC" name="一个简单流程" isExecutable="true">
    <extensionElements>
        <flowable:executionListener event="start" delegateExpression="${processListener}"/>
        <flowable:executionListener event="end" delegateExpression="${processListener}"/>
    </extensionElements>
</process>
*/

/*@Component("processListener")
public class ProcessListener  implements ExecutionListener {
    @Override
    public void notify(DelegateExecution delegateExecution) {
        String eventName = delegateExecution.getEventName();
        FlowElement element = delegateExecution.getCurrentFlowElement();
        Map<String, VariableInstance> variableInstances = delegateExecution.getVariableInstances();
        Map<String, Object> vars = delegateExecution.getVariables();
        delegateExecution.setVariable("variableSetInExecutionListener", "firstValue");
        delegateExecution.setVariable("eventReceived", delegateExecution.getEventName());
    }
}*/


