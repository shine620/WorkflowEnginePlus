package com.hy.workflow.service;

import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.model.ApproveRequest;
import org.flowable.bpmn.model.*;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
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



}
