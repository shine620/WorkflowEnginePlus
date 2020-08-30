package com.hy.workflow.service;

import com.hy.workflow.base.SpringContextUtil;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.repository.BusinessProcessRepository;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.impl.FlowableMultiInstanceActivityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;


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

    }


    private void taskCreated(TaskEntityImpl taskEntity){

        logger.error("任务生成监听事件执行 processInstanceId: "+taskEntity.getProcessInstanceId() +"  taskId:"+taskEntity.getId());
        Map taskKeyVariable = taskEntity.getVariable(taskEntity.getTaskDefinitionKey(),Map.class);
        List<Map<String,Object>> callActivityList = taskEntity.getVariable("callActivityList",List.class);
        //通过流程变量设置任务处理人
         if(taskKeyVariable!=null){  //普通任务节点(通过任务类型排除会签)
            if(FlowElementType.USER_TASK.equals(taskKeyVariable.get("flowElementType"))){
                String assignee = (String)taskKeyVariable.get("assignee");
                List<String> candidateUser = (List)taskKeyVariable.get("candidateUser");
                List<String> candidateGroup = (List)taskKeyVariable.get("candidateGroup");
                if(assignee!=null) taskEntity.setAssignee(assignee);
                if(candidateUser!=null) taskEntity.addCandidateUsers(candidateUser);
                if(candidateGroup!=null) taskEntity.addCandidateGroups(candidateGroup);
            }
        }
         //调用活动多实例生成的任务节点
        else if (callActivityList!=null && callActivityList.size()>0 ){
            //通过 任务Key、父节点ID、子流程模型Key
            Iterator<Map<String,Object>> it = callActivityList.listIterator();
            while (it.hasNext()){
                System.out.println(taskEntity.getVariable("callActivityList",List.class));
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
                            if (assignee != null) taskEntity.setAssignee(assignee);
                            if (candidateGroup != null) taskEntity.addCandidateGroups(candidateGroup);
                            if (candidateUser != null) taskEntity.addCandidateUsers(candidateUser);
                            it.remove();
                            break;
                        }
                    }
                }
            }
        }

    }


    private void taskCompleted(TaskEntityImpl taskEntity){
        logger.error("任务完成监听事件执行 processInstanceId: "+taskEntity.getProcessInstanceId() +"  taskId:"+taskEntity.getId());
        Map<String, Object>  variables = taskEntity.getVariables();
    }


    private void multiInstanceActivityStarted(FlowableMultiInstanceActivityEventImpl multiIEvent){
        logger.error("多实例开始 processInstanceId: "+multiIEvent.getProcessInstanceId() +"  activityId:"+multiIEvent.getActivityId());
        DelegateExecution execution = multiIEvent.getExecution();
        //调用活动多实例
        if(FlowElementType.CALL_ACTIVITY.equals(multiIEvent.getActivityType())){
            Map callActivityModelIdMap = execution.getVariable("callActivityModelKeyMap",Map.class);
            if(callActivityModelIdMap!=null){
                List modelIdList = (List) callActivityModelIdMap.get(multiIEvent.getActivityId());
                if(modelIdList!=null&&modelIdList.size()>0){
                    execution.setVariable("subProcessDefinitionKeyList",modelIdList);
                }
            }
        }
        //普通任务多实例
        else{
            Map userVariable = execution.getVariable(multiIEvent.getActivityId(),Map.class);
            if(userVariable!=null&&userVariable.size()>0){
                List<String> assigneeList = (List)userVariable.get("assigneeList");
                execution.setVariable("assigneeList",assigneeList);
            }
        }
    }


    private void processStarted(ExecutionEntityImpl execution){
        logger.error("流程发起监听事件执行 processInstanceId: "+execution.getProcessInstanceId());
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
        logger.error("流程结束监听事件执行 processInstanceId: "+execution.getProcessInstanceId());
        //TODO  此处应回调业务接口回写数据状态
    }


    private void historicProcessInstanceEnded(HistoricProcessInstanceEntityImpl execution){
        logger.error("历史实例结束监听事件执行 processInstanceId: "+execution.getProcessInstanceId());
        Optional<BusinessProcess> op = businessProcessRepository.findById(execution.getProcessInstanceId());
        if(op.isPresent()){
            BusinessProcess bp = op.get();
            bp.setEnded(true);
            bp.setEndTime(execution.getEndTime());
            businessProcessRepository.save(bp);
        }
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


