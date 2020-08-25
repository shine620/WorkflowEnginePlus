package com.hy.workflow.service;

import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.FlowElementModel;
import org.apache.commons.collections.CollectionUtils;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.flowable.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.flowable.engine.impl.util.condition.ConditionUtil;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.jws.soap.SOAPBinding;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 流程引擎Service
 * @author zhoayao
 * @version 1.0
 *
 */
@Service
@Transactional
public class WorkflowService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;


    public List<FlowElementModel> getFirstNode(String processDefinitionId) {

        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if(model == null)  throw new WorkflowException("该流程定义没有模型数据："+processDefinitionId);

        List<FlowElementModel> flowList =  new ArrayList<>();
        Process process =  model.getMainProcess();
        FlowElement startNode = process.getInitialFlowElement();
        FlowNode firstNode =(FlowNode) ((StartEvent) startNode).getOutgoingFlows().get(0).getTargetFlowElement();
        List<SequenceFlow> outgoingFlows = firstNode.getOutgoingFlows();

        for (SequenceFlow outgoingFlow : outgoingFlows) {
            FlowElement targetFlowElement = outgoingFlow.getTargetFlowElement();
            if (targetFlowElement instanceof UserTask) {
                FlowElementModel flow = new FlowElementModel();
                UserTask userTask =  (UserTask)targetFlowElement;
                flow.setId(userTask.getId());
                flow.setName(userTask.getName());
                flow.setFlowElementType(FlowElementType.USER_TASK);
                flowList.add(flow);
            }else{
                throw new WorkflowException("第二节点只能是用户任务节点！");
            }
        }
        return flowList;
    }




}
