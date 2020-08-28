package com.hy.workflow.model;

import com.hy.workflow.enums.ActivityType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.List;
import java.util.Map;

@ApiModel(value="StartProcessRequest对象",description="封装流程发起请求参数")
public class StartProcessRequest {

    @ApiModelProperty(value="流程定义ID",required = true, example="Model100:2:120052")
    private String processDefinitionId;

    @ApiModelProperty(value="流程发起人ID",required = true,example="zhangsan")
    private String startUserId;

    @ApiModelProperty(value="业务ID",required = true, example="1000")
    private String businessId;

    @ApiModelProperty(value="业务类型",required = true, example="CONTRACT")
    private String businessType;

    @ApiModelProperty(value="业务名称",required = true, example="房屋租赁合同")
    private String businessName;

    @ApiModelProperty(value="业务URL地址",example="las/viewContract/1000")
    private String businessUrl;

    @ApiModelProperty(value="流程变量")
    private Map<String,Object> variables;

    @ApiModelProperty(value="下一环节")
    private List<NextTask> nextTaskList;


    public static class NextTask{

        public NextTask(){}

        @ApiModelProperty(value="下一节点ID",example="BuMenJingLi")
        private String activityId;

        @ApiModelProperty(value="下一节点审批人",example="zhangsan")
        private String assignee;

        @ApiModelProperty(value="下一节点候选人",example="['wangwu','lisi']")
        private List<String> candidateUser;

        @ApiModelProperty(value="下一节点候选组",example="['LasGroup']")
        private List<String> candidateGroup;

        @ApiModelProperty(value="下一节点类型",example=ActivityType.USER_TASK)
        private String activityType;

        public String getActivityId() {return activityId; }
        public void setActivityId(String activityId) {  this.activityId = activityId; }

        public String getAssignee() { return assignee;}
        public void setAssignee(String assignee) {this.assignee = assignee;}

        public List<String> getCandidateUser() {return candidateUser; }
        public void setCandidateUser(List<String> candidateUser) {this.candidateUser = candidateUser; }

        public List<String> getCandidateGroup() {return candidateGroup;  }
        public void setCandidateGroup(List<String> candidateGroup) { this.candidateGroup = candidateGroup; }

        public String getActivityType() { return activityType; }
        public void setActivityType(String activityType) { this.activityType = activityType; }

    }

    public List<NextTask> getNextTaskList() {
        return nextTaskList;
    }

    public void setNextTaskList(List<NextTask> nextTaskList) {
        this.nextTaskList = nextTaskList;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessName() {
        return businessName;
    }

    public void setBusinessName(String businessName) {
        this.businessName = businessName;
    }

    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }


}
