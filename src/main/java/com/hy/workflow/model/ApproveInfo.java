package com.hy.workflow.model;

import com.hy.workflow.enums.FlowElementType;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

public class ApproveInfo {

    @ApiModelProperty(value="流程变量")
    private Map<String,Object> variables;

    @ApiModelProperty(value="审批意见",example="同意")
    private String opinion;

    @ApiModelProperty(value="当前用户ID",example="张三")
    private String userId;

    @ApiModelProperty(value="下一环节")
    private List<NextTask> nextTaskList;

    public static class NextTask{

        public NextTask(){}

        @ApiModelProperty(value="下一节点ID",example="BuMenJingLi")
        private String flowElementId;

        @ApiModelProperty(value="下一节点审批人",example="zhangsan")
        private String assignee;

        @ApiModelProperty(value="下一节点候选人",example="['wangwu','lisi']")
        private List<String> candidateUser;

        @ApiModelProperty(value="下一节点候选组",example="['LasGroup']")
        private List<String> candidateGroup;

        @ApiModelProperty(value="下一节点类型",example= FlowElementType.USER_TASK)
        private String flowElementType;

        @ApiModelProperty(value="父节点ID",example= "CaiWuTasK")
        private String parentFlowElementId;

        @ApiModelProperty(value="父节点类型",example= FlowElementType.CALL_ACTIVITY)
        private String parentFlowElementType;

        @ApiModelProperty(value="模型Key",example="Model1000")
        private String modelKey;

        @ApiModelProperty(value="组ID",example="ChengBanRen")
        private String  groupId;

        @ApiModelProperty(value="子流程部门ID",example="1002")
        private String  suProcessDepartmentId;


        public String getFlowElementId() {
            return flowElementId;
        }

        public void setFlowElementId(String flowElementId) {
            this.flowElementId = flowElementId;
        }

        public String getAssignee() {
            return assignee;
        }

        public void setAssignee(String assignee) {
            this.assignee = assignee;
        }

        public List<String> getCandidateUser() {
            return candidateUser;
        }

        public void setCandidateUser(List<String> candidateUser) {
            this.candidateUser = candidateUser;
        }

        public List<String> getCandidateGroup() {
            return candidateGroup;
        }

        public void setCandidateGroup(List<String> candidateGroup) {
            this.candidateGroup = candidateGroup;
        }

        public String getFlowElementType() {
            return flowElementType;
        }

        public void setFlowElementType(String flowElementType) {
            this.flowElementType = flowElementType;
        }

        public String getParentFlowElementType() {
            return parentFlowElementType;
        }

        public void setParentFlowElementType(String parentFlowElementType) {
            this.parentFlowElementType = parentFlowElementType;
        }

        public String getModelKey() {
            return modelKey;
        }

        public void setModelKey(String modelKey) {
            this.modelKey = modelKey;
        }

        public String getParentFlowElementId() {
            return parentFlowElementId;
        }

        public void setParentFlowElementId(String parentFlowElementId) {
            this.parentFlowElementId = parentFlowElementId;
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getSuProcessDepartmentId() {
            return suProcessDepartmentId;
        }

        public void setSuProcessDepartmentId(String suProcessDepartmentId) {
            this.suProcessDepartmentId = suProcessDepartmentId;
        }

    }


    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

    public String getOpinion() {
        return opinion;
    }

    public void setOpinion(String opinion) {
        this.opinion = opinion;
    }

    public List<NextTask> getNextTaskList() {
        return nextTaskList;
    }

    public void setNextTaskList(List<NextTask> nextTaskList) {
        this.nextTaskList = nextTaskList;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }


}
