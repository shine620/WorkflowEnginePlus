package com.hy.workflow.service;

import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.enums.ActivityType;
import com.hy.workflow.repository.BusinessProcessRepository;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEntityEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEvent;
import org.flowable.common.engine.api.delegate.event.FlowableEventType;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.event.AbstractFlowableEngineEventListener;
import org.flowable.engine.delegate.event.impl.FlowableMultiInstanceActivityEventImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.task.service.impl.persistence.entity.TaskEntityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
            DelegateExecution execution = multiIEvent.getExecution();
            Map userVariable = execution.getVariable(multiIEvent.getActivityId(),Map.class);
            List<String> assigneeList = (List)userVariable.get("assigneeList");
            execution.setVariable("assigneeList",assigneeList);
        }
        //任务完成
        else if(FlowableEngineEventType.TASK_COMPLETED.equals(eventType)){
            taskCompleted( (TaskEntityImpl)flowEvent.getEntity() );
        }

    }


    private void taskCreated(TaskEntityImpl taskEntity){
        logger.error("任务生成监听事件执行 processInstanceId: "+taskEntity.getProcessInstanceId() +"  taskId:"+taskEntity.getId());
        //通过流程变量设置任务处理人
        Map userVariable = taskEntity.getVariable(taskEntity.getTaskDefinitionKey(),Map.class);
        if(userVariable!=null){
            //普通任务
            if(ActivityType.USER_TASK.equals(userVariable.get("activityType"))){
                String assignee = (String)userVariable.get("assignee");
                List<String> candidateUser = (List)userVariable.get("candidateUser");
                List<String> candidateGroup = (List)userVariable.get("candidateGroup");
                if(assignee!=null) taskEntity.setAssignee(assignee);
                if(candidateUser!=null) taskEntity.addCandidateUsers(candidateUser);
                if(candidateGroup!=null) taskEntity.addCandidateGroups(candidateGroup);
            }
        }
        System.out.println(userVariable);
    }

    private void taskCompleted(TaskEntityImpl taskEntity){
        logger.error("任务完成监听事件执行 processInstanceId: "+taskEntity.getProcessInstanceId() +"  taskId:"+taskEntity.getId());
        Map<String, Object>  variables = taskEntity.getVariables();

    }

    private void processStarted(ExecutionEntityImpl execution){
        logger.error("流程发起监听事件执行 processInstanceId: "+execution.getProcessInstanceId());
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


