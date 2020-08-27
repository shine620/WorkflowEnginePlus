package com.hy.workflow.service;

import com.hy.workflow.base.FindNextUserTaskNodeCmd;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.ApproveRequest;
import com.hy.workflow.model.FlowElementConfigModel;
import com.hy.workflow.model.FlowElementModel;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.util.EntityModelUtil;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;


    /**
     * 审批任务
     *
     * @author  zhaoyao
     * @param approveRequest 审批请求数据
     * @return List<FlowElementModel>
     */
    public void completeTask(ApproveRequest approveRequest) {
        ApproveType approveType = approveRequest.getApproveType();
        //生成审批意见
        taskService.addComment(approveRequest.getTaskId(), approveRequest.getProcessInstanceId(), approveRequest.getOpinion());
        //审批通过
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


    /**
     * 根据当前任务ID查询下一个审批节点
     *
     * @author  zhaoyao
     * @param taskId 任务ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getNextUserTask(String taskId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        ExecutionEntity execution = (ExecutionEntity) runtimeService.createExecutionQuery().executionId(task.getExecutionId()).singleResult();
        BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
        List<UserTask> userTaskList = managementService.executeCommand(new FindNextUserTaskNodeCmd(execution, bpmnModel));
        List<FlowElementModel> flowList = new ArrayList<>();
        List<String> flowIdList =  new ArrayList<>();
        userTaskList.forEach(userTask -> {
            FlowElementModel model =  new FlowElementModel();
            model.setId(userTask.getId());
            model.setName(userTask.getName());
            MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
            if(multiInstance!=null){
                if(multiInstance.isSequential()) model.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                else model.setFlowElementType(FlowElementType.PARALLEL_TASK);
            }else{
                model.setFlowElementType(FlowElementType.USER_TASK);
            }
            flowList.add(model);
            flowIdList.add(userTask.getId());
        });
        //查询并填充节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);
        return flowList;
    }



}
