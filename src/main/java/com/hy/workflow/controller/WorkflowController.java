package com.hy.workflow.controller;

import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.FlowElementModel;
import io.swagger.annotations.*;
import org.flowable.bpmn.model.*;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.impl.EngineInfo;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngines;
import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
@RequestMapping("/WorkflowController")
@Api(value = "流程引擎", tags = "Workflows", description = "流程引擎接口")
public class WorkflowController {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;

    @Autowired
    private RepositoryService repositoryService;

    @ApiOperation(value = "List engine properties", tags = { "Workflows" })
    @GetMapping(value = "/management/properties", produces = "application/json")
    public Map<String, String> getProperties() {
        return managementService.getProperties();
    }

    @ApiOperation(value = "Get FlowElement", tags = { "Workflows" })
    @GetMapping(value = "/workflows/{processDefinitionId}", produces = "application/json")
    public List<FlowElementModel> getFlowElement(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        List<FlowElementModel> flowElementList = new ArrayList<>();
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if(model != null) {
            Collection<FlowElement> flowElements = model.getMainProcess().getFlowElements();
            for(FlowElement e : flowElements) {
                FlowElementModel flowElement = new FlowElementModel();
                if (e instanceof StartEvent) {
                    flowElement.setFlowElementType(FlowElementType.START_EVENT);
                }else if(e instanceof UserTask){
                    flowElement.setFlowElementType(FlowElementType.SERVICE_TASK);
                }else if(e instanceof ServiceTask){
                    flowElement.setFlowElementType(FlowElementType.USER_TASK);
                }else if(e instanceof CallActivity){
                    flowElement.setFlowElementType(FlowElementType.CALL_ACTIVITY);
                }else if(e instanceof SubProcess){
                    flowElement.setFlowElementType(FlowElementType.SUB_PROCESS);
                }else if(e instanceof EndEvent){
                    flowElement.setFlowElementType(FlowElementType.END_EVENT);
                }
                if(flowElement.getFlowElementType()!=null){
                    flowElement.setId(e.getId());
                    flowElement.setName(e.getName());
                    flowElementList.add(flowElement);
                }
                System.out.println("flowelement id:" + e.getId() + "  name:" + e.getName() + "   class:" + e.getClass().toString());
            }
        }
        return flowElementList;
    }


}
