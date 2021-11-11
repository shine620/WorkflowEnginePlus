package com.hy.workflow.controller;

import com.hy.workflow.common.base.BaseRequest;
import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.BusinessType;
import com.hy.workflow.entity.FlowableModel;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.model.FlowElementModel;
import com.hy.workflow.model.ModelRequest;
import com.hy.workflow.model.ProcessInstanceModel;
import com.hy.workflow.service.WorkflowService;
import io.swagger.annotations.*;
import org.flowable.bpmn.model.*;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.*;

@RestController
@RequestMapping("/WorkflowController")
@Api(value = "流程引擎", tags = "Workflows", description = "流程相关接口")
public class WorkflowController {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;

    @Autowired
    protected WorkflowService workflowService;

    @Autowired
    private RepositoryService repositoryService;

    @ApiOperation(value = "流程引擎属性列表", tags = { "Workflows" })
    @GetMapping(value = "/management/properties", produces = "application/json")
    public Map<String, String> getProperties() {
        return managementService.getProperties();
    }


    @ApiOperation(value = "获取流程定义的节点列表", tags = { "Workflows" })
    @GetMapping(value = "/workflows/{processDefinitionId}", produces = "application/json")
    public Map<String,Object> getFlowElement(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        List<FlowElementModel> flowElements = new ArrayList<>();
        BpmnModel model = repositoryService.getBpmnModel(processDefinitionId);
        if(model != null) {
            Collection<FlowElement> listFlowElement = model.getMainProcess().getFlowElements();
            listFlowElement(listFlowElement,flowElements);
        }
        Map map = new HashMap();
        map.put("msg","OK");
        map.put("code","200");
        map.put("count",flowElements.size());
        map.put("data",flowElements);
        return map;
    }

    private List<FlowElementModel> listFlowElement( Collection<FlowElement> flowElements,List<FlowElementModel> flowElementList  ) {
        for(FlowElement e : flowElements) {
            FlowElementModel flowElement = new FlowElementModel();
            if (e instanceof StartEvent) {
                //flowElement.setFlowElementType(FlowElementType.START_EVENT);
            }else if(e instanceof UserTask){
                flowElement.setFlowElementType(FlowElementType.SERVICE_TASK);
            }else if(e instanceof ServiceTask){
                flowElement.setFlowElementType(FlowElementType.USER_TASK);
            }else if(e instanceof CallActivity){
                flowElement.setFlowElementType(FlowElementType.CALL_ACTIVITY);
            }else if(e instanceof SubProcess){
                flowElement.setFlowElementType(FlowElementType.SUB_PROCESS);
                Collection<FlowElement> sub =((SubProcess) e).getFlowElements();
                listFlowElement(sub,flowElementList);
            }else if(e instanceof EndEvent){
                //flowElement.setFlowElementType(FlowElementType.END_EVENT);
            }
            if(flowElement.getFlowElementType()!=null){
                flowElement.setFlowElementId(e.getId());
                flowElement.setFlowElementName(e.getName());
                if(e.getParentContainer() instanceof SubProcess){
                    flowElement.setParentFlowElementId(((SubProcess) e.getParentContainer()).getId());
                }else{
                    flowElement.setParentFlowElementId("0");
                }
                flowElementList.add(flowElement);
            }
        }
        return flowElementList;
    }


    @ApiOperation(value = "获取业务类型列表", tags = { "Workflows" })
    @PostMapping("/workflows/findBusinessTypes")
    public PageBean<BusinessType> findBusinessTypes(@ApiParam @RequestParam(defaultValue = "1") Integer startPage,
            @ApiParam @RequestParam(defaultValue = "10") Integer pageSize,@RequestBody BaseRequest baseRequest) {
        PageRequest pageRequest = PageBean.getPageRequest(baseRequest,startPage,pageSize);
        return workflowService.findBusinessType(baseRequest,pageRequest);
    }


    @ApiOperation(value = "获取所有业务类型", tags = { "Workflows" })
    @PostMapping("/workflows/getAllBusinessType")
    public List<BusinessType> getAllBusinessType(){
        return workflowService.getAllBusinessType();
    }


    @ApiOperation(value = "根据ID查询业务类型", tags = {"Workflows" })
    @GetMapping(value = "/workflows/getBusinessType/{id}",consumes ="application/json" )
    public BusinessType getBusinessType(@PathVariable String id){
        return workflowService.getBusinessType(id);
    }


    @ApiOperation(value = "保存业务类型(创建或修改)", tags = {"Workflows" })
    @PostMapping(value = "/workflows/saveBusinessType",consumes ="application/json" )
    public BusinessType saveBusinessType(@RequestBody BusinessType businessType){
        return workflowService.saveBusinessType(businessType);
    }


    @ApiOperation(value = "删除业务类型", tags = { "Workflows" })
    @GetMapping("/workflows/deleteBusinessType/{id}")
    public void deleteBusinessType(@ApiParam(name = "id") @PathVariable String id, HttpServletResponse response) {
        workflowService.deleteBusinessType(id);
        response.setStatus(HttpStatus.NO_CONTENT.value());

    }


    @ApiOperation(value = "批量删除业务类型", tags = { "Workflows" })
    @GetMapping("/workflows/batchDeleteBusinessType")
    public void batchDeleteBusinessType(@ApiParam(name = "ids") @RequestParam String[] ids, HttpServletResponse response) {
        workflowService.batchDeleteBusinessType(ids);
        response.setStatus(HttpStatus.NO_CONTENT.value());
    }


    @ApiOperation(value = "查找流程业务关联数据", tags = { "Workflows" })
    @GetMapping(value = "/workflows/findBusinessProcess", produces = "application/json")
    public List<BusinessProcess> findBusinessProcess(@ApiParam(name = "businessId",value = "业务ID") @RequestParam String businessId,
                                              @ApiParam(name = "businessType",value = "业务类型") @RequestParam String businessType) {
        return workflowService.findBusinessProcess(businessId,businessType);
    }


}
