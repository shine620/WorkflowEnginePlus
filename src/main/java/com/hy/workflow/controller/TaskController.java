package com.hy.workflow.controller;

import com.hy.workflow.model.FlowElementConfigModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.flowable.engine.RepositoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/TaskController")
@Api(value = "流程任务", tags = "Tasks", description = "流程任务接口")
public class TaskController {

    @Autowired
    private RepositoryService repositoryService;

    @ApiOperation(value = "Get a task", notes="获取一个节点配置",tags = { "Tasks" })
    @GetMapping(value = "/tasks/getTaskInfo", produces = "application/json")
    public Map<String,Object> getTaskInfo(
            @ApiParam(name = "processDefinitionId",value = "流程实例ID") @RequestParam String processInstanceId,
            @ApiParam(name = "flowElementId",value = "流程任务ID") @RequestParam String taskId) {

        return null;
    }


}
