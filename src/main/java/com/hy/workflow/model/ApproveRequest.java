package com.hy.workflow.model;

import com.hy.workflow.enums.ApproveType;

import java.util.Map;

public class ApproveRequest {

    private String processInstanceId;

    private String taskId;

    private String opinion;

    private Map<String,Object> variables;

    private ApproveType approveType;

    private String rejectActivityId;

    private String turnUserId;


    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public ApproveType getApproveType() {
        return approveType;
    }

    public void setApproveType(ApproveType approveType) {
        this.approveType = approveType;
    }

    public String getRejectActivityId() {
        return rejectActivityId;
    }

    public void setRejectActivityId(String rejectActivityId) {
        this.rejectActivityId = rejectActivityId;
    }

    public String getTurnUserId() {
        return turnUserId;
    }

    public void setTurnUserId(String turnUserId) {
        this.turnUserId = turnUserId;
    }


}
