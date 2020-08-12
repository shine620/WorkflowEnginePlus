package com.hy.workflow.entity;

import com.hy.workflow.model.FlowElementConfigModel;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class FlowElementConfig {

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid") //这个是hibernate的注解/生成32位UUID
    @GeneratedValue(generator="idGenerator")
    private String id;

    //节点ID
    private String flowElementId;

    //任务名称
    private String flowElementName;

    //流程定义ID
    private String processDefinitionId;

    //流程定义名称
    private String processDefinitionName;

    //节点类型
    private String flowElementType;

    //是否会签
    private String multiInstanceType;

    //处理人选择方式
    private String assigneeSelectOption;

    //候选范围
    private String assigneeSelectScope;

    //候选人员
    private String candidateUsers;

    //候选角色
    private String candidateRoles;

    //候选机构
    private String candidateGroups;

    //部门职位
    private String candidateJob;

    //表单字段
    private String formField;

    //是否允许编辑表单
    private Boolean editForm;

    //是否允许编辑正文
    private Boolean attachEditable;

    //审批意见是否必填
    private Boolean requireOpinion;

    //是否显示审批单记录
    private Boolean showApproveRecord;

    //是否允许驳回
    private Boolean rejectable;

    //是否允许抄送
    private Boolean sendCopy;

    //是否允许抄送
    private String tip;

    //创建时间
    private Date createTime;

    //修改时间
    private Date updateTime;


    public FlowElementConfig(){}

    public FlowElementConfig(FlowElementConfigModel model) {
        if(model==null) return;
        BeanUtils.copyProperties(model,this);
    }

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

    public String getFlowElementName() {
        return flowElementName;
    }

    public void setFlowElementName(String flowElementName) {
        this.flowElementName = flowElementName;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public String getFlowElementType() {
        return flowElementType;
    }

    public void setFlowElementType(String flowElementType) {
        this.flowElementType = flowElementType;
    }

    public String getMultiInstanceType() {
        return multiInstanceType;
    }

    public void setMultiInstanceType(String multiInstanceType) {
        this.multiInstanceType = multiInstanceType;
    }

    public String getAssigneeSelectOption() {
        return assigneeSelectOption;
    }

    public void setAssigneeSelectOption(String assigneeSelectOption) {
        this.assigneeSelectOption = assigneeSelectOption;
    }

    public String getAssigneeSelectScope() {
        return assigneeSelectScope;
    }

    public void setAssigneeSelectScope(String assigneeSelectScope) {
        this.assigneeSelectScope = assigneeSelectScope;
    }

    public String getCandidateUsers() {
        return candidateUsers;
    }

    public void setCandidateUsers(String candidateUsers) {
        this.candidateUsers = candidateUsers;
    }

    public String getCandidateRoles() {
        return candidateRoles;
    }

    public void setCandidateRoles(String candidateRoles) {
        this.candidateRoles = candidateRoles;
    }

    public String getCandidateGroups() {
        return candidateGroups;
    }

    public void setCandidateGroups(String candidateGroups) {
        this.candidateGroups = candidateGroups;
    }

    public String getCandidateJob() {
        return candidateJob;
    }

    public void setCandidateJob(String candidateJob) {
        this.candidateJob = candidateJob;
    }

    public String getFormField() {
        return formField;
    }

    public void setFormField(String formField) {
        this.formField = formField;
    }

    public Boolean getEditForm() {
        return editForm;
    }

    public void setEditForm(Boolean editForm) {
        this.editForm = editForm;
    }

    public Boolean getAttachEditable() {
        return attachEditable;
    }

    public void setAttachEditable(Boolean attachEditable) {
        this.attachEditable = attachEditable;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }


}
