package com.hy.workflow.service;

import com.hy.workflow.base.FindNextUserTaskNodeCmd;
import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveRequest;
import com.hy.workflow.model.FlowElementModel;
import org.apache.commons.collections.CollectionUtils;
import org.flowable.bpmn.model.*;
import org.flowable.engine.*;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;

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


    public void completeTask(ApproveRequest approveRequest) {

        ApproveType approveType = approveRequest.getApproveType();
        //审批意见
        taskService.addComment(approveRequest.getTaskId(), approveRequest.getProcessInstanceId(), approveRequest.getOpinion());

        //审批
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


    public List<FlowElementModel> getNextUserTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List<UserTask> userTaskList = managementService.executeCommand(new FindNextUserTaskNodeCmd(execution, bpmnModel));
        List<FlowElementModel> flowList = new ArrayList<>();
        userTaskList.forEach(userTask -> {
            FlowElementModel model =  new FlowElementModel();
            model.setId(userTask.getId());
            model.setName(userTask.getName());
            model.setFlowElementType(FlowElementType.USER_TASK);
            flowList.add(model);
        });
        return flowList;
    }


}
