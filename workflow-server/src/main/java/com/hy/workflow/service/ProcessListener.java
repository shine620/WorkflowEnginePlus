package com.hy.workflow.service;

import com.alibaba.fastjson.JSONArray;
import com.hy.workflow.common.util.SpringContextUtil;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.MultiInstanceRecord;
import com.hy.workflow.entity.TaskRecord;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.TaskRecordRepository;
import com.hy.workflow.util.HttpUtil;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import java.util.*;


/* *
    ========================== ??????????????? ======================
    ?????????????????????????????????????????????FlowableEntityEventImpl???org.flowable.engine.delegate.event.impl.FlowableEntityEventImp
    ???????????????FlowableEntityEventImpl???org.flowable.common.engine.impl.event.FlowableEntityEventImp
*/

@Component("processListener")
public class ProcessListener extends AbstractFlowableEngineEventListener {

    private static final Logger logger = LoggerFactory.getLogger(ProcessListener.class);

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private TaskRecordRepository taskRecordRepository;

    @Value("${callback.address}")
    private String callbackAddress;

    @Override
    public void onEvent(FlowableEvent event) {

        FlowableEventType eventType = event.getType();
        FlowableEngineEntityEvent flowEvent =  (event instanceof  FlowableEngineEntityEvent) ? (FlowableEngineEntityEvent)event : null;

        //????????????
        if(FlowableEngineEventType.PROCESS_STARTED.equals(eventType)){
            processStarted( (ExecutionEntityImpl)flowEvent.getEntity() );
        }
        //????????????
        else if(FlowableEngineEventType.PROCESS_COMPLETED.equals(eventType)){
            processCompleted( (ExecutionEntityImpl)flowEvent.getEntity() );
        }
        //??????????????????PROCESS_COMPLETED?????????????????????????????????????????????????????????????????????????????????
        // ?????????????????????????????????(??????????????????)?????????????????????
        else if(FlowableEngineEventType.HISTORIC_PROCESS_INSTANCE_ENDED.equals(eventType)){
            HistoricProcessInstanceEntityImpl execution = (HistoricProcessInstanceEntityImpl)flowEvent.getEntity();
            historicProcessInstanceEnded(execution);
        }
        //????????????
        else if(FlowableEngineEventType.TASK_CREATED.equals(eventType)){
            taskCreated( (TaskEntityImpl)flowEvent.getEntity() );
        }
        //???????????????(?????????????????????)
        else if(FlowableEngineEventType.MULTI_INSTANCE_ACTIVITY_STARTED.equals(eventType)){
            FlowableMultiInstanceActivityEventImpl multiIEvent= (FlowableMultiInstanceActivityEventImpl)event;
            multiInstanceActivityStarted(multiIEvent);
        }
        //????????????
        else if(FlowableEngineEventType.TASK_COMPLETED.equals(eventType)){
            taskCompleted( (TaskEntityImpl)flowEvent.getEntity() );
        }
        //??????????????????????????????
        else if(FlowableEngineEventType.TASK_ASSIGNED.equals(eventType)){
            taskAssigned((TaskEntityImpl)flowEvent.getEntity());
        }
        //????????????(??????????????????????????????)
        else if(FlowableEngineEventType.ACTIVITY_CANCELLED.equals(eventType)){
            FlowableActivityCancelledEventImpl activityCancelled = (FlowableActivityCancelledEventImpl) event;
            activityCancelled(activityCancelled);
        }
        //???????????????(?????????????????????)
        else if(FlowableEngineEventType.TASK_OWNER_CHANGED.equals(eventType)){
            taskOwnerChanged((TaskEntityImpl)flowEvent.getEntity());
        }
    }


    private void taskCreated(TaskEntityImpl taskEntity){
        logger.info("?????????????????????????????? processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        //????????????????????????????????????????????????????????????????????????????????????????????????ID????????????????????????????????????????????????ID????????????
        Map taskKeyVariable = taskEntity.getVariable(taskEntity.getTaskDefinitionKey(),Map.class);
        List<Map<String,Object>> callActivityList = taskEntity.getVariable("callActivityList",List.class);
        TaskRecord taskRecord = new TaskRecord();
        //???????????????????????????????????????
         if(taskKeyVariable!=null){  //??????????????????(??????????????????????????????)
            if(FlowElementType.USER_TASK.equals(taskKeyVariable.get("flowElementType"))){
                String assignee = (String)taskKeyVariable.get("assignee");
                List<String> candidateUser = (List)taskKeyVariable.get("candidateUser");
                List<String> candidateGroup = (List)taskKeyVariable.get("candidateGroup");
                //if(assignee!=null) taskEntity.setAssignee(assignee); ?????????????????????????????????????????????????????????????????????????????????????????????
                TaskService taskService = SpringContextUtil.getBeanByClass(TaskService.class);
                if(assignee!=null) taskService.setAssignee(taskEntity.getId(),assignee);
                if(candidateUser!=null) taskEntity.addCandidateUsers(candidateUser);
                if(candidateGroup!=null) taskEntity.addCandidateGroups(candidateGroup);
                //???????????????
                taskRecord.setAssignee(assignee);
                taskRecord.setCandidateUser(StringUtils.join(candidateUser,","));
                taskRecord.setCandidateGroup(StringUtils.join(candidateGroup,","));
                //?????????????????????????????????????????????????????????
                taskEntity.removeVariable(taskEntity.getTaskDefinitionKey());
            }
        }
         //??????????????????????????????????????????
        else if (callActivityList!=null && callActivityList.size()>0 ){
            //?????? ??????Key????????????ID??????????????????Key???????????????????????????
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
                            //???????????????
                            taskRecord.setAssignee(assignee);
                            taskRecord.setCandidateUser(StringUtils.join(candidateUser,","));
                            taskRecord.setCandidateGroup(StringUtils.join(candidateGroup,","));
                            taskRecord.setSubProcessDepartmentId((String)map.get("subProcessDepartmentId"));
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }
        //??????????????????
        saveTaskRecord(taskRecord,taskEntity);
    }


    private void taskCompleted(TaskEntityImpl taskEntity){
        logger.info("?????????????????????????????? processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
        TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
        if(taskRecord!=null){ //???????????????????????????????????????????????????????????????????????????????????????????????????1?????????
            taskRecord.setEndTime(new Date());
            entityManager.merge(taskRecord);
        }
        //TODO ??????????????????
        RuntimeService runtimeService =SpringContextUtil.getBeanByClass(RuntimeService.class);
        ProcessInstance currentInstance = runtimeService.createProcessInstanceQuery().processInstanceId(taskEntity.getProcessInstanceId()).singleResult();
        if(currentInstance!=null){
            Map params = new HashMap();
            params.put("taskId",taskEntity.getId());
            params.put("taskDefinitionKey",taskEntity.getTaskDefinitionKey());
            params.put("taskName",taskEntity.getName());
            params.put("assignee",taskEntity.getAssignee());
            params.put("processInstanceId",currentInstance.getId());
            params.put("processInstanceName",currentInstance.getName());
            params.put("processDefinitionId",currentInstance.getProcessDefinitionId());
            params.put("startUserId",currentInstance.getStartUserId());
            params.put("processStartTime",currentInstance.getStartTime());
            params.put("businessKey",currentInstance.getBusinessKey());
            params.put("eventType",FlowableEngineEventType.TASK_COMPLETED.name());
            callback(params);
        }
    }


    private void taskOwnerChanged(TaskEntityImpl taskEntity){
        logger.info("????????????????????? processInstanceId:{}  taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
        TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
        if(taskRecord!=null){
            taskRecord.setOwner(taskEntity.getOwner());
            entityManager.merge(taskRecord);
        }
    }


    private void activityCancelled(FlowableActivityCancelledEventImpl activityCancelled){
        logger.info("????????????{}???processInstanceId={}???executionId={}",activityCancelled.getClass(),activityCancelled.getProcessInstanceId(),activityCancelled.getExecutionId());
        if(activityCancelled.getCause() instanceof UserTask){
            TaskRecord taskRecord = taskRecordRepository.findByExecutionIdAndTaskDefinitionKey(activityCancelled.getExecutionId(),activityCancelled.getActivityId());
            if(taskRecord!=null){
                taskRecord.setEndTime(new Date());
                taskRecord.setCancelled(true);
                taskRecordRepository.save(taskRecord);
            }
        }
    }


    private void multiInstanceActivityStarted(FlowableMultiInstanceActivityEventImpl multiIEvent){
        logger.info("??????????????? processInstanceId:{}  activityId:{}",multiIEvent.getProcessInstanceId(),multiIEvent.getActivityId());
        DelegateExecution execution = multiIEvent.getExecution();
        MultiInstanceRecord record = new MultiInstanceRecord();
        //?????????????????????
        if(FlowElementType.CALL_ACTIVITY.equals(multiIEvent.getActivityType())){
            Map callActivityModelIdMap = execution.getVariable("callActivityModelKeyMap",Map.class);
            if(callActivityModelIdMap!=null){
                List modelIdList = (List) callActivityModelIdMap.get(multiIEvent.getActivityId());
                if(modelIdList!=null&&modelIdList.size()>0){
                    execution.setVariable("subProcessDefinitionKeyList",modelIdList);
                    //???????????????????????????
                    record.setSubProcessDefinitionKeyList( JSONArray.toJSON(modelIdList).toString() );
                    //???????????????????????????????????????
                    callActivityModelIdMap.remove(multiIEvent.getActivityId());
                }
            }
        }
        //?????????????????????
        else{
            Map userVariable = execution.getVariable(multiIEvent.getActivityId(),Map.class);
            if(userVariable!=null&&userVariable.size()>0){
                List<String> assigneeList = (List)userVariable.get("assigneeList");
                execution.setVariable("assigneeList",assigneeList);
                //?????????????????????
                if( assigneeList!=null && assigneeList.size()>0 ) record.setAssigneeList( JSONArray.toJSON(assigneeList).toString() );
                //???????????????????????????????????????????????????
                execution.removeVariable(multiIEvent.getActivityId());
            }
        }
        //??????????????????
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
        logger.info("?????????????????????????????? processInstanceId:"+execution.getProcessInstanceId());
        ExecutionEntityImpl subProcessInstance = execution.getParent();
        if(subProcessInstance.getSuperExecution()!=null){ //?????????????????????????????????
            ExecutionEntity rootProcessInstance = execution.getRootProcessInstance();
            String subProcessName = rootProcessInstance.getName()+"-"+subProcessInstance.getProcessDefinitionName();
            subProcessInstance.setStartUserId(rootProcessInstance.getStartUserId());
            subProcessInstance.setName(subProcessName);
            execution.setStartUserId(rootProcessInstance.getStartUserId());
            execution.setName(subProcessName);
            RuntimeService runtimeService =SpringContextUtil.getBeanByClass(RuntimeService.class);
            runtimeService.setProcessInstanceName(subProcessInstance.getId(),subProcessName);
        }
        //TODO  ?????????????????????????????????????????????-
        else{
            ExecutionEntity root = execution.getRootProcessInstance();
            Map<String,Object> params = new HashMap<>();
            params.put("processInstanceId",root.getId());
            params.put("processInstanceName",root.getName());
            params.put("processDefinitionId",root.getProcessDefinitionId());
            params.put("processStartTime",root.getStartTime());
            params.put("processStartUserId",root.getStartUserId());
            params.put("businessKey",root.getBusinessKey());
            params.put("eventType",FlowableEngineEventType.PROCESS_STARTED.name());
            callback(params);
        }
    }


    private void processCompleted(ExecutionEntityImpl execution){
        logger.info("?????????????????????????????? processInstanceId: "+execution.getProcessInstanceId());
        //TODO  ?????????????????????????????????????????????
        if(execution.getSuperExecution()==null){
            ExecutionEntity root = execution.getRootProcessInstance();
            Map<String,Object> params = new HashMap<>();
            params.put("processInstanceId",root.getId());
            params.put("processInstanceName",root.getName());
            params.put("processDefinitionId",root.getProcessDefinitionId());
            params.put("processStartTime",root.getStartTime());
            params.put("processStartUserId",root.getStartUserId());
            params.put("businessKey",root.getBusinessKey());
            params.put("eventType",FlowableEngineEventType.PROCESS_COMPLETED.name());
            callback(params);
        }
    }


    private void historicProcessInstanceEnded(HistoricProcessInstanceEntityImpl execution){
        logger.info("???????????????????????????????????? processInstanceId:"+execution.getProcessInstanceId());
        Optional<BusinessProcess> op = businessProcessRepository.findById(execution.getProcessInstanceId());
        if(op.isPresent()){
            BusinessProcess bp = op.get();
            bp.setEnded(true);
            bp.setEndTime(execution.getEndTime());
            businessProcessRepository.save(bp);
        }
    }


    //????????????????????????
    private void taskAssigned(TaskEntityImpl taskEntity ){
        logger.info("?????????????????????????????????????????? processInstanceId:{}???taskId:{}",taskEntity.getProcessInstanceId(),taskEntity.getId());
        Optional<TaskRecord> optional = taskRecordRepository.findById( taskEntity.getId() );
        if(optional.isPresent()){
            TaskRecord taskRecord = optional.get();
            taskRecord.setAssignee(taskEntity.getAssignee());
            taskRecord.setClaimTime(taskEntity.getClaimTime());
            taskRecordRepository.save(taskRecord);
        }
    }


    //??????????????????
    private void deleteTaskRecord(FlowableEngineEntityEvent flowEvent){
        if(flowEvent.getEntity() instanceof HistoricTaskInstanceEntityImpl){
            HistoricTaskInstanceEntityImpl taskEntity = (HistoricTaskInstanceEntityImpl) flowEvent.getEntity();
            if(taskEntity.isDeleted()){
                logger.info("???????????????{},taskId={}???processInstanceId={}",taskEntity.getName(),taskEntity.getId(),taskEntity.getProcessInstanceId());
                /*EntityManager entityManager = SpringContextUtil.getBeanByClass(EntityManager.class);
                TaskRecord taskRecord = entityManager.find(TaskRecord.class,taskEntity.getId());
                if(taskRecord!=null){
                    entityManager.remove(taskRecord);
                }*/
                taskRecordRepository.deleteById(taskEntity.getId());
            }
        }
    }


    //??????????????????
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


    private void callback(Map<String,Object> params){
        if(StringUtils.isNotBlank(callbackAddress)){
            System.out.println(callbackAddress);
            logger.info(params.toString());
            String result = HttpUtil.post(callbackAddress,params.toString(),null);
            logger.info(result);
        }
    }


}





/*
 ????????????????????????????????????????????????????????????????????????xml???
<process id="YGJDLC" name="??????????????????" isExecutable="true">
    <extensionElements>
        <flowable:eventListener events="PROCESS_STARTED" delegateExpression="${processListener}"/>
        <flowable:eventListener events="PROCESS_COMPLETED" delegateExpression="${processListener}"/>
    </extensionElements>
</process>
*/


/* *
    ======================???????????????????????????????????????????????????start???end???take????????? ======================
*/

/*
<process id="YGJDLC" name="??????????????????" isExecutable="true">
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


