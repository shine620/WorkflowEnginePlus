package com.hy.workflow.model;

import com.hy.workflow.common.base.BaseRequest;


public class FlowElementConfigModel extends BaseRequest {


    private String id;

    //节点ID
    private String flowElementId;

    //流程定义ID
    private String processDefinitionId;

    //节点类型
    private String flowElementType;

    //是否多人
    private Boolean multiUser;

    //是否固定
    private Boolean fixed;

    //候选方式
    private String assigneeOption;

    //机构范围（发起人所在部门 START_DEPARTMENT、发起人所在公司 START_UNIT 、指定机构 GIVEN_ORG）
    private String orgScope;

    //选择的机构
    private String orgValue;

    //选择的用户
    private String userValue;

    //选择的角色
    private String roleValue;

    //选择的岗位
    private String positionValue;

    //自动带出
    private Boolean autoSelect;

    //是否允许编辑表单
    private Boolean editForm;

    //意见是否必填
    private Boolean requireOpinion;

    //是否显示审批单记录
    private Boolean showApproveRecord;

    //是否允许驳回
    private Boolean rejectable;

    //是否允许抄送
    private Boolean sendCopy;

    //审批提示
    private String tip;



    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFlowElementId() {
        return flowElementId;
    }

    public void setFlowElementId(String flowElementId) {
        this.flowElementId = flowElementId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getFlowElementType() {
        return flowElementType;
    }

    public void setFlowElementType(String flowElementType) {
        this.flowElementType = flowElementType;
    }

    public Boolean getMultiUser() {
        return multiUser;
    }

    public void setMultiUser(Boolean multiUser) {
        this.multiUser = multiUser;
    }

    public Boolean getFixed() {
        return fixed;
    }

    public void setFixed(Boolean fixed) {
        this.fixed = fixed;
    }

    public String getAssigneeOption() {
        return assigneeOption;
    }

    public void setAssigneeOption(String assigneeOption) {
        this.assigneeOption = assigneeOption;
    }

    public String getOrgScope() {
        return orgScope;
    }

    public void setOrgScope(String orgScope) {
        this.orgScope = orgScope;
    }

    public String getOrgValue() {
        return orgValue;
    }

    public void setOrgValue(String orgValue) {
        this.orgValue = orgValue;
    }

    public String getUserValue() {
        return userValue;
    }

    public void setUserValue(String userValue) {
        this.userValue = userValue;
    }

    public String getPositionValue() {
        return positionValue;
    }

    public void setPositionValue(String positionValue) {
        this.positionValue = positionValue;
    }

    public Boolean getAutoSelect() {
        return autoSelect;
    }

    public void setAutoSelect(Boolean autoSelect) {
        this.autoSelect = autoSelect;
    }

    public Boolean getEditForm() {
        return editForm;
    }

    public void setEditForm(Boolean editForm) {
        this.editForm = editForm;
    }

    public Boolean getRequireOpinion() {
        return requireOpinion;
    }

    public void setRequireOpinion(Boolean requireOpinion) {
        this.requireOpinion = requireOpinion;
    }

    public Boolean getShowApproveRecord() {
        return showApproveRecord;
    }

    public void setShowApproveRecord(Boolean showApproveRecord) {
        this.showApproveRecord = showApproveRecord;
    }

    public Boolean getRejectable() {
        return rejectable;
    }

    public void setRejectable(Boolean rejectable) {
        this.rejectable = rejectable;
    }

    public Boolean getSendCopy() {
        return sendCopy;
    }

    public void setSendCopy(Boolean sendCopy) {
        this.sendCopy = sendCopy;
    }

    public String getTip() {
        return tip;
    }

    public void setTip(String tip) {
        this.tip = tip;
    }

    public String getRoleValue() {
        return roleValue;
    }

    public void setRoleValue(String roleValue) {
        this.roleValue = roleValue;
    }


}
