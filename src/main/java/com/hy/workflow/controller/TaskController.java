package com.hy.workflow.controller;

import com.hy.workflow.model.ApproveRequest;
import com.hy.workflow.model.TaskModel;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkType;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/TaskController")
@Api(value = "流程任务", tags = "Tasks", description = "流程任务接口")
public class TaskController {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private TaskService taskService;


    @ApiOperation(value = "Get todo task list", notes="查询待办列表",tags = { "Tasks" })
    @GetMapping(value = "/tasks/getTodoTaskList", produces = "application/json")
    public Map<String,Object> getTodoTaskList(
         @ApiParam(name = "userId",value = "用户ID") @RequestParam String userId,@ApiParam @RequestParam(defaultValue = "false") Boolean loadAll,
         @ApiParam @RequestParam(defaultValue = "1") Integer startPage,@ApiParam @RequestParam(defaultValue = "10") Integer pageSize) {

        ArrayList taskList =  new ArrayList();
        TaskQuery taskQuery  = taskService.createTaskQuery()
                .taskCandidateOrAssigned(userId)
                .orderByTaskCreateTime()
                .desc();
        Long totalCount = taskQuery.count();

        List<Task> todoTaskList ;
        if(loadAll==true){
            todoTaskList = taskQuery.list();
        }else{
            int startIndex = (startPage-1)*20;
            todoTaskList = taskQuery.listPage(startIndex,pageSize);
        }

        todoTaskList.forEach(task -> {
            TaskModel taskModel = new TaskModel();
            taskModel.setTaskId(task.getId());
            taskModel.setTaskName(task.getName());
            taskModel.setTaskDefinitionKey(task.getTaskDefinitionKey());
            taskModel.setCreateTime(task.getCreateTime());
            taskModel.setAssignee(task.getAssignee());
            taskList.add(taskModel);
        });

        Map result = new HashMap();
        result.put("totalCount",totalCount);
        result.put("datas",taskList);
        return result;
    }


    @ApiOperation(value = "Claim Task", notes="签收任务",tags = { "Tasks" })
    @GetMapping(value = "/tasks/claimTask", produces = "application/json")
    public void claimTask(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId,
                          @ApiParam(name = "userId",value = "签收人ID") @RequestParam String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if(task==null) throw new RuntimeException("签收失败，该任务不存在："+taskId);
        taskService.claim(taskId, userId);
    }


    @ApiOperation(value = "Cancel Claim Task", notes="反签收任务",tags = { "Tasks" })
    @GetMapping(value = "/tasks/unClaimTask", produces = "application/json")
    public void unClaimTask(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId) {
        //检查候选人和候选组
        List<IdentityLink> identityLinks = taskService.getIdentityLinksForTask(taskId);
        boolean flag = false;
        if (CollectionUtils.isNotEmpty(identityLinks)) {
            for (IdentityLink link : identityLinks) {
                if (IdentityLinkType.CANDIDATE.equals(link.getType())) {
                    flag = true;
                    break;
                }
            }
        }
        //反签收
        if (flag) {
            taskService.claim(taskId, null);
        } else {
            throw  new RuntimeException("没有候选人或候选组,不能进行反签收操作");
        }
    }


    @ApiOperation(value = "Set Assignee", notes="设置处理人",tags = { "Tasks" })
    @GetMapping(value = "/tasks/setAssignee", produces = "application/json")
    public void setAssignee(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId,
                          @ApiParam(name = "userId",value = "处理人ID") @RequestParam String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if(task==null) throw new RuntimeException("设置处理人失败，该任务不存在："+taskId);
        taskService.setAssignee(taskId,userId);
    }


    @ApiOperation(value = "Approved task", notes="任务审批",tags = { "Tasks" })
    @PostMapping(value = "/tasks/approvedTask", produces = "application/json")
    public void approvedTask(@RequestBody ApproveRequest approveRequest) {
        Task task = taskService.createTaskQuery().taskId(approveRequest.getTaskId()).singleResult();
        if(task==null) throw new RuntimeException("审批失败，该任务不存在："+approveRequest.getTaskId());
        //taskService.addComment(task.getId(), approveRequest.getProcessInstanceId(), approveRequest.getOpinion());
        taskService.complete(task.getId(),approveRequest.getVariables());
    }







}
