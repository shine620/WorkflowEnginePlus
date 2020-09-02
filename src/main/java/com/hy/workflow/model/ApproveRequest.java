package com.hy.workflow.model;

import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

@ApiModel(value="ApproveRequest对象",description="封装流程审批请求参数")
public class ApproveRequest extends ApproveInfo{

    @ApiModelProperty(value="流程实例ID",required = true, example="170052")
    private String processInstanceId;

    @ApiModelProperty(value="任务ID",required = true, example="1200")
    private String taskId;

    @ApiModelProperty(value="审批类型")
    private ApproveType approveType;


    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }



    public ApproveType getApproveType() {
        return approveType;
    }

    public void setApproveType(ApproveType approveType) {
        this.approveType = approveType;
    }


}
