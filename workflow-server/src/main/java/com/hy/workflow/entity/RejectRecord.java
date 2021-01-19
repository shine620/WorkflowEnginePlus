package com.hy.workflow.entity;

import com.hy.workflow.enums.RejectType;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
public class RejectRecord implements Serializable {

    @Id
    @GenericGenerator(name="idGenerator",strategy = "org.hibernate.id.UUIDGenerator")
    @GeneratedValue(generator="idGenerator")
    private String id;

    //流程实例ID
    private String processInstanceId;

    //业务ID
    private String businessId;

    //业务类型
    private String businessType;

    //驳回类型
    @Enumerated(EnumType.STRING)
    private RejectType rejectType;

    //创建时间
    private Date createTime;

    //驳回用户
    private String rejectUser;

    //驳回时任务ID
    private String sourceTaskId;

    //驳回环节ID
    private String sourceElementId;

    //驳回环节名称
    private String sourceElementName;

    //被驳回环节ID
    private String targetElementId;

    //被驳回环节名称
    private String targetElementName;

    //被驳回环节的流程实例ID
    private String targetProcessInstanceId;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
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

    public RejectType getRejectType() {
        return rejectType;
    }

    public void setRejectType(RejectType rejectType) {
        this.rejectType = rejectType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getRejectUser() {
        return rejectUser;
    }

    public void setRejectUser(String rejectUser) {
        this.rejectUser = rejectUser;
    }

    public String getSourceTaskId() {
        return sourceTaskId;
    }

    public void setSourceTaskId(String sourceTaskId) {
        this.sourceTaskId = sourceTaskId;
    }

    public String getSourceElementId() {
        return sourceElementId;
    }

    public void setSourceElementId(String sourceElementId) {
        this.sourceElementId = sourceElementId;
    }

    public String getSourceElementName() {
        return sourceElementName;
    }

    public void setSourceElementName(String sourceElementName) {
        this.sourceElementName = sourceElementName;
    }

    public String getTargetElementId() {
        return targetElementId;
    }

    public void setTargetElementId(String targetElementId) {
        this.targetElementId = targetElementId;
    }

    public String getTargetElementName() {
        return targetElementName;
    }

    public void setTargetElementName(String targetElementName) {
        this.targetElementName = targetElementName;
    }

    public String getTargetProcessInstanceId() {
        return targetProcessInstanceId;
    }

    public void setTargetProcessInstanceId(String targetProcessInstanceId) {
        this.targetProcessInstanceId = targetProcessInstanceId;
    }

}
