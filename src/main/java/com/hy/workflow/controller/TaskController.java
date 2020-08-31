package com.hy.workflow.controller;

import com.hy.workflow.base.PageBean;
import com.hy.workflow.base.WorkflowException;
import com.hy.workflow.model.ApproveRequest;
import com.hy.workflow.model.FlowElementModel;
import com.hy.workflow.model.TaskModel;
import com.hy.workflow.service.FlowableTaskService;
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

    @Autowired
    private FlowableTaskService flowableTaskService;


    @ApiOperation(value = "查询待办列表", tags = { "Tasks" })
    @GetMapping(value = "/tasks/todoTaskList", produces = "application/json")
    public PageBean<TaskModel> getTodoTaskList(
         @ApiParam @RequestParam(defaultValue = "false") Boolean loadAll,@ApiParam(name = "userId",value = "用户ID") @RequestParam String userId,
         @ApiParam @RequestParam(defaultValue = "1") Integer pageNum,@ApiParam @RequestParam(defaultValue = "10") Integer pageSize) {

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
            int startIndex = (pageNum-1)*20;
            todoTaskList = taskQuery.listPage(startIndex,pageSize);
        }

        todoTaskList.forEach(task -> {
            TaskModel taskModel = new TaskModel();
            taskModel.setTaskId(task.getId());
            taskModel.setTaskName(task.getName());
            taskModel.setTaskDefinitionKey(task.getTaskDefinitionKey());
            taskModel.setCreateTime(task.getCreateTime());
            taskModel.setAssignee(task.getAssignee());
            taskModel.setProcessInstanceId(task.getProcessInstanceId());
            taskList.add(taskModel);
        });

        PageBean taskPage = new PageBean(pageNum,pageSize,totalCount);
        taskPage.setData(taskList);
        return taskPage;
    }


    @ApiOperation(value = "签收任务", tags = { "Tasks" })
    @GetMapping(value = "/tasks/claimTask", produces = "application/json")
    public void claimTask(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId,
                          @ApiParam(name = "userId",value = "签收人ID") @RequestParam String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if(task==null) throw new WorkflowException("签收失败，该任务不存在："+taskId);
        taskService.claim(taskId, userId);
    }


    @ApiOperation(value = "反签收任务", tags = { "Tasks" })
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
            throw  new WorkflowException("没有候选人或候选组,不能进行反签收操作");
        }
    }


    @ApiOperation(value = "设置处理人", tags = { "Tasks" })
    @GetMapping(value = "/tasks/setAssignee", produces = "application/json")
    public void setAssignee(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId,
                          @ApiParam(name = "userId",value = "处理人ID") @RequestParam String userId) {
        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        if(task==null) throw new WorkflowException("设置处理人失败，该任务不存在："+taskId);
        taskService.setAssignee(taskId,userId);
    }


    @ApiOperation(value = "审批任务", tags = { "Tasks" })
    @PostMapping(value = "/tasks/approvedTask", produces = "application/json")
    public void approvedTask(@RequestBody ApproveRequest approveRequest) {
        Task task = taskService.createTaskQuery().taskId(approveRequest.getTaskId()).singleResult();
        if(task==null) throw new WorkflowException("审批失败，该任务不存在："+approveRequest.getTaskId());
        flowableTaskService.completeTask(approveRequest);
    }


    @ApiOperation(value = "获取第一个审批节点", tags = { "Tasks" })
    @GetMapping(value = "/tasks/getFirstNode/{processDefinitionId}", produces = "application/json")
    public List<FlowElementModel> getFirstNode(@ApiParam(name = "processDefinitionId") @PathVariable String processDefinitionId) {
        List<FlowElementModel> firstFlowList = flowableTaskService.getFirstNode(processDefinitionId);
        return firstFlowList;
    }


    @ApiOperation(value = "根据当前任务获取下一审批节点",tags = { "Tasks" })
    @GetMapping(value = "/tasks/getNextUserTask", produces = "application/json")
    public List<FlowElementModel>  getNextUserTask(@ApiParam(name = "taskId",value = "任务ID") @RequestParam String taskId) {
        List<FlowElementModel>  flowList =  flowableTaskService.getNextUserTask(taskId);
        return flowList;
    }

    @ApiOperation(value = "根据部门ID获取该部门子流程审批节点",tags = { "Tasks" })
    @GetMapping(value = "/tasks/getSubProcessByDeptId", produces = "application/json")
    public List<FlowElementModel>  getSubProcessByDeptId(@ApiParam(name = "departmentId",value = "部门ID") @RequestParam String departmentId) {
        List<FlowElementModel>  flowList =  flowableTaskService.getSubProcessByDeptId(departmentId);
        return flowList;
    }


}
