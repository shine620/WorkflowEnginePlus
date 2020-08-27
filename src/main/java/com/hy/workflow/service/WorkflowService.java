package com.hy.workflow.service;

import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.FlowElementModel;
import com.hy.workflow.repository.FlowElementConfigRepository;
import com.hy.workflow.util.EntityModelUtil;
import org.flowable.bpmn.model.*;
import org.flowable.bpmn.model.Process;
import org.flowable.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

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
    private FlowElementConfigRepository flowElementConfigRepository;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;


    /**
     * 获取第一个审批节点
     *
     * @author  zhaoyao
     * @param processDefinitionId 流程定义ID
     * @return List<FlowElementModel>
     */
    public List<FlowElementModel> getFirstNode(String processDefinitionId) {

        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if(model == null)  throw new WorkflowException("该流程定义没有模型数据："+processDefinitionId);

        List<FlowElementModel> flowList =  new ArrayList<>();
        List<String> flowIdList =  new ArrayList<>();

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
                MultiInstanceLoopCharacteristics multiInstance = userTask.getLoopCharacteristics();
                if(multiInstance!=null){
                    if(multiInstance.isSequential()) flow.setFlowElementType(FlowElementType.SEQUENTIAL_TASK);
                    else flow.setFlowElementType(FlowElementType.PARALLEL_TASK);
                }else{
                    flow.setFlowElementType(FlowElementType.USER_TASK);
                }
                flowList.add(flow);
                flowIdList.add(flow.getId());
            }else{
                throw new WorkflowException("第二节点只能是用户任务节点！");
            }
        }
        //查询节点配置信息
        List<FlowElementConfig> configs = flowElementConfigRepository.findByFlowElementIdIn(flowIdList);
        EntityModelUtil.fillFlowElementConfig(flowList,configs);

        return flowList;
    }




}
