package com.hy.workflow.entity;

import com.hy.workflow.enums.RejectType;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class RejectRecord {

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid")
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
    private String rejectTaskId;

    //驳回环节ID
    private String rejectElementId;

    //驳回环节名称
    private String rejectElementName;

    //被驳回环节ID
    private String rejectedElementId;

    //被驳回环节名称
    private String rejectedElementName;


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

    public String getRejectTaskId() {
        return rejectTaskId;
    }

    public void setRejectTaskId(String rejectTaskId) {
        this.rejectTaskId = rejectTaskId;
    }

    public String getRejectElementId() {
        return rejectElementId;
    }

    public void setRejectElementId(String rejectElementId) {
        this.rejectElementId = rejectElementId;
    }

    public String getRejectElementName() {
        return rejectElementName;
    }

    public void setRejectElementName(String rejectElementName) {
        this.rejectElementName = rejectElementName;
    }

    public String getRejectedElementId() {
        return rejectedElementId;
    }

    public void setRejectedElementId(String rejectedElementId) {
        this.rejectedElementId = rejectedElementId;
    }

    public String getRejectedElementName() {
        return rejectedElementName;
    }

    public void setRejectedElementName(String rejectedElementName) {
        this.rejectedElementName = rejectedElementName;
    }

}
