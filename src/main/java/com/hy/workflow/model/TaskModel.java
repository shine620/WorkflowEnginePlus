package com.hy.workflow.model;

import com.hy.workflow.enums.MultiInstanceType;

import java.util.Date;

public class TaskModel {


    private String taskId;

    private String taskName;

    private String processInstanceId;

    private String assignee;

    private String taskDefinitionKey;

    private Date createTime;

    private MultiInstanceType multiInstanceType;


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

    public String getAssignee() {
        return assignee;
    }

    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public MultiInstanceType getMultiInstanceType() {
        return multiInstanceType;
    }

    public void setMultiInstanceType(MultiInstanceType multiInstanceType) {
        this.multiInstanceType = multiInstanceType;
    }


}
