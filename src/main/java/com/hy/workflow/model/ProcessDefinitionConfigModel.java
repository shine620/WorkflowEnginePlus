package com.hy.workflow.model;

import java.util.Date;

public class ProcessDefinitionConfigModel {

    private String processDefinitionId;

    private String processDefinitionKey;

    private String processDefinitionName;

    private Integer version;

    private String description;

    private Boolean suspended;

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

    public ProcessDefinitionConfigModel(){ }

    //构造器参数顺序要与findProcessDefinitionConfigLaterstList方法查询顺序一致
    public ProcessDefinitionConfigModel(
                String processDefinitionId, String processDefinitionKey, String processDefinitionName,
                Integer version, String description, Boolean suspended, String createUserId, String createDeptId, String createUnitId,
                Date createTime, Date updateTime, String businessType, String departmentId, String unitId, String deploymentId,
                Boolean callable, Boolean defaultProcess) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionKey = processDefinitionKey;
        this.processDefinitionName = processDefinitionName;
        this.version = version;
        this.description = description;
        this.suspended = suspended;
        this.createUserId = createUserId;
        this.createDeptId = createDeptId;
        this.createUnitId = createUnitId;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.businessType = businessType;
        this.departmentId = departmentId;
        this.unitId = unitId;
        this.deploymentId = deploymentId;
        this.callable = callable;
        this.defaultProcess = defaultProcess;
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

    public Boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(Boolean suspended) {
        this.suspended = suspended;
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
