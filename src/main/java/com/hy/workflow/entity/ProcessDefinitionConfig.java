package com.hy.workflow.entity;


import com.hy.workflow.model.ProcessDefinitionConfigModel;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
public class ProcessDefinitionConfig {

    @Id
    private String processDefinitionId;

    private String processDefinitionKey;

    private String processDefinitionName;

    private Integer version;

    @Column(length = 2000)
    private String description;

    private String suspensionState;

    private String createUserId;

    private String createDeptId;

    private String createUnitId;

    private Date createTime;

    private Date updateTime;

    private String businessType;

    private String departmentId;

    private String unitId;

    private String deploymentId;

    private Boolean callable;

    private Boolean defaultProcess;

    public ProcessDefinitionConfig(){ };

    public ProcessDefinitionConfig(ProcessDefinitionConfigModel pdcModel) {
        if(pdcModel==null) return;
        this.processDefinitionId = pdcModel.getProcessDefinitionId();
        this.processDefinitionKey = pdcModel.getProcessDefinitionKey();
        this.processDefinitionName = pdcModel.getProcessDefinitionName();
        this.version = pdcModel.getVersion();
        this.description = pdcModel.getDescription();
        this.suspensionState = pdcModel.getSuspensionState();
        this.createUserId = pdcModel.getCreateUserId();
        this.createDeptId = pdcModel.getCreateDeptId();
        this.createUnitId = pdcModel.getCreateUnitId();
        this.createTime = pdcModel.getCreateTime();
        this.updateTime = pdcModel.getUpdateTime();
        this.businessType = pdcModel.getBusinessType();
        this.departmentId = pdcModel.getDepartmentId();
        this.unitId = pdcModel.getUnitId();
        this.deploymentId = pdcModel.getDeploymentId();
        this.callable = pdcModel.getCallable();
        this.defaultProcess = pdcModel.getDefaultProcess();
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getProcessDefinitionKey() {
        return processDefinitionKey;
    }

    public void setProcessDefinitionKey(String processDefinitionKey) {
        this.processDefinitionKey = processDefinitionKey;
    }

    public String getProcessDefinitionName() {
        return processDefinitionName;
    }

    public void setProcessDefinitionName(String processDefinitionName) {
        this.processDefinitionName = processDefinitionName;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuspensionState() {
        return suspensionState;
    }

    public void setSuspensionState(String suspensionState) {
        this.suspensionState = suspensionState;
    }

    public String getCreateUserId() {
        return createUserId;
    }

    public void setCreateUserId(String createUserId) {
        this.createUserId = createUserId;
    }

    public String getCreateDeptId() {
        return createDeptId;
    }

    public void setCreateDeptId(String createDeptId) {
        this.createDeptId = createDeptId;
    }

    public String getCreateUnitId() {
        return createUnitId;
    }

    public void setCreateUnitId(String createUnitId) {
        this.createUnitId = createUnitId;
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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getUnitId() {
        return unitId;
    }

    public void setUnitId(String unitId) {
        this.unitId = unitId;
    }

    public String getDeploymentId() {
        return deploymentId;
    }

    public void setDeploymentId(String deploymentId) {
        this.deploymentId = deploymentId;
    }

    public Boolean getCallable() {
        return callable;
    }

    public void setCallable(Boolean callable) {
        this.callable = callable;
    }

    public Boolean getDefaultProcess() {
        return defaultProcess;
    }

    public void setDefaultProcess(Boolean defaultProcess) {
        this.defaultProcess = defaultProcess;
    }


}
