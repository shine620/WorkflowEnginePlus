package com.hy.workflow.model;


import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Date;

@ApiModel(value="任务信息",description="待办或已办信息封装对象")
public class TaskModel {

    @ApiModelProperty(value="任务ID",example="1200")
    private String taskId;

    @ApiModelProperty(value="任务名称", example="部门经理审批")
    private String taskName;

    @ApiModelProperty(value="任务Key", example="JingLiNode")
    private String taskDefinitionKey;

    @ApiModelProperty(value="流程实例ID",example="170052")
    private String processInstanceId;

    @ApiModelProperty(value="流程实例名称", example="技术产权合同-北大英华合同审批流程")
    private String processInstanceName;

    @ApiModelProperty(value="流程定义ID",example="Model170005:6:277508")
    private String processDefinitionId;

    @ApiModelProperty(value="处理人",example="zhangsan")
    private String assignee;

    @ApiModelProperty(value="所属人",example="xiaoming")
    private String owner;

    @ApiModelProperty(value="创建时间")
    private Date createTime;

    @ApiModelProperty(value="签收时间")
    private Date claimTime;

    @ApiModelProperty(value="结束时间")
    private Date endTime;

    @ApiModelProperty(value="持续时长",notes = "创建到结束的时长")
    private Long durationInMillis;

    @ApiModelProperty(value="处理时长",notes = "签收到结束的时长")
    private Long workTimeInMillis;

    @ApiModelProperty(value="执行实例ID")
    private String executionId;



    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskName() {
        return taskName;
    }

    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceName() {
        return processInstanceName;
    }

    public void setProcessInstanceName(String processInstanceName) {
        this.processInstanceName = processInstanceName;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getClaimTime() {
        return claimTime;
    }

    public void setClaimTime(Date claimTime) {
        this.claimTime = claimTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Long getDurationInMillis() {
        return durationInMillis;
    }

    public void setDurationInMillis(Long durationInMillis) {
        this.durationInMillis = durationInMillis;
    }

    public Long getWorkTimeInMillis() {
        return workTimeInMillis;
    }

    public void setWorkTimeInMillis(Long workTimeInMillis) {
        this.workTimeInMillis = workTimeInMillis;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getExecutionId() {
        return executionId;
    }

    public void setExecutionId(String executionId) {
        this.executionId = executionId;
    }
}
