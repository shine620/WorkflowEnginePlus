package com.hy.workflow.model;

import com.hy.workflow.enums.ApproveType;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

public class ApproveRequest {


    @ApiModelProperty(value="流程实例ID",required = true, example="170052")
    private String processInstanceId;

    @ApiModelProperty(value="任务ID",required = true, example="1200")
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
