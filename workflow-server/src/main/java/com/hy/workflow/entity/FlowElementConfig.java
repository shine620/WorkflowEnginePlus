package com.hy.workflow.entity;

import com.hy.workflow.common.base.BaseEntity;
import com.hy.workflow.model.FlowElementConfigModel;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.beans.BeanUtils;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
public class FlowElementConfig  extends BaseEntity{

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid") //这个是hibernate的注解/生成32位UUID
    @GeneratedValue(generator="idGenerator")
    private String id;

    //节点ID
    private String flowElementId;

    //流程定义ID
    private String processDefinitionId;

    //节点类型
    private String flowElementType;

    //是否多人
    private Boolean multUser;

    //是否固定
    private Boolean fixed;

    //候选方式
    private String assigneeOption;

    //候选值
    private String assigneeValue;

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

    public Boolean getMultUser() {
        return multUser;
    }

    public void setMultUser(Boolean multUser) {
        this.multUser = multUser;
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

    public String getAssigneeValue() {
        return assigneeValue;
    }

    public void setAssigneeValue(String assigneeValue) {
        this.assigneeValue = assigneeValue;
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

}
