package com.hy.workflow.model;

import com.hy.workflow.enums.FlowElementType;
import com.hy.workflow.enums.RejectPosition;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;

@ApiModel(value="RejectTask对象",description="某个任务的可驳回节点")
public class RejectTask {

    @ApiModelProperty(value="当前任务ID",example="172001")
    private String taskId;

    @ApiModelProperty(value="当前任务名称",example="经理审批")
    private String taskName;

    @ApiModelProperty(value="当前任务Key",example="JingLi")
    private String taskDefinitionKey;

    @ApiModelProperty(value="历史审批环节")
    private List<TaskNode> hisTask;



    public static class TaskNode{

        @ApiModelProperty(value="节点ID",example="12000")
        private String flowElementId;

        @ApiModelProperty(value="节点名称",example="承办人发起")
        private String flowElementName;

        @ApiModelProperty(value="节点名称",example= FlowElementType.USER_TASK)
        private String flowElementType;

        @ApiModelProperty(value="父流程实例ID",notes = "有值时说明子流程驳回到父流程节点", example="11000")
        private String parentProcessInstanceId;

        @ApiModelProperty(value="驳回节点的位置",notes = "并行网关前、并行网关后、无并行网关", example= RejectPosition.NO_GATEWAY)
        private String rejectPosition;

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

        public String getParentProcessInstanceId() {
            return parentProcessInstanceId;
        }

        public void setParentProcessInstanceId(String parentProcessInstanceId) {
            this.parentProcessInstanceId = parentProcessInstanceId;
        }

        public String getRejectPosition() {
            return rejectPosition;
        }

        public void setRejectPosition(String rejectPosition) {
            this.rejectPosition = rejectPosition;
        }

    }

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

    public String getTaskDefinitionKey() {
        return taskDefinitionKey;
    }

    public void setTaskDefinitionKey(String taskDefinitionKey) {
        this.taskDefinitionKey = taskDefinitionKey;
    }

    public List<TaskNode> getHisTask() {
        return hisTask;
    }

    public void setHisTask(List<TaskNode> hisTask) {
        this.hisTask = hisTask;
    }


}
