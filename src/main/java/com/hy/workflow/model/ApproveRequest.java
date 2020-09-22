package com.hy.workflow.model;

import com.hy.workflow.enums.ApproveType;
import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.enums.RejectPosition;
import com.hy.workflow.enums.RejectType;
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

    @ApiModelProperty(value="驳回信息")
    private RejectInfo rejectInfo;

    public static class RejectInfo{

        @ApiModelProperty(value="驳回类型")
        private RejectType rejectType;

        @ApiModelProperty(value="驳回节点ID", example="JingLi")
        private String flowElementId;

        @ApiModelProperty(value="驳回节点名称", example="经理审批")
        private String flowElementName;

        @ApiModelProperty(value="驳回节点类型", example=FlowElementType.USER_TASK)
        private String flowElementType;

        @ApiModelProperty(value="驳回节点的位置",notes = "并行网关前、并行网关后、无并行网关", example= RejectPosition.NO_GATEWAY)
        private String rejectPosition;

        @ApiModelProperty(value="主流程实例ID",notes = "有值时为主流程节点", example="11000")
        private String parentProcessInstanceId;

        public RejectType getRejectType() {
            return rejectType;
        }

        public void setRejectType(RejectType rejectType) {
            this.rejectType = rejectType;
        }

        public String getParentProcessInstanceId() {
            return parentProcessInstanceId;
        }

        public void setParentProcessInstanceId(String parentProcessInstanceId) {
            this.parentProcessInstanceId = parentProcessInstanceId;
        }

        public String getFlowElementId() {
            return flowElementId;
        }

        public void setFlowElementId(String flowElementId) {
            this.flowElementId = flowElementId;
        }

        public String getFlowElementName() {
            return flowElementName;
        }

        public void setFlowElementName(String flowElementName) {
            this.flowElementName = flowElementName;
        }

        public String getFlowElementType() {
            return flowElementType;
        }

        public void setFlowElementType(String flowElementType) {
            this.flowElementType = flowElementType;
        }

        public String getRejectPosition() {
            return rejectPosition;
        }

        public void setRejectPosition(String rejectPosition) {
            this.rejectPosition = rejectPosition;
        }
    }

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

    public RejectInfo getRejectInfo() {
        return rejectInfo;
    }

    public void setRejectInfo(RejectInfo rejectInfo) {
        this.rejectInfo = rejectInfo;
    }

}
