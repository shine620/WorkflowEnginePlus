package com.hy.workflow.model;

import com.hy.workflow.common.base.BaseRequest;

import java.util.Date;

public class DefinitionConfigModel extends BaseRequest {

    private String processDefinitionId;

    private String processDefinitionKey;

    private String processDefinitionName;

    private Integer version;

    private String description;

    private Boolean suspended;

    private String createUser;

    private String updateUser;

    private Date createTime;

    private Date updateTime;

    private String businessType;

    private String departmentId;

    private String unitId;

    private String deploymentId;

    private Boolean callable;

    private Boolean defaultProcess;

    private Boolean rejectParentProcess;

    private Boolean rejectGatewayBefore;


    public DefinitionConfigModel(){ }

    //构造器参数顺序要与findProcessDefinitionConfigLaterstList方法查询顺序一致
    public DefinitionConfigModel(
                String processDefinitionId, String processDefinitionKey, String processDefinitionName,
                Integer version, String description, Boolean suspended, String createUser, String updateUser,
                Date createTime, Date updateTime, String businessType, String departmentId, String unitId, String deploymentId,
                Boolean callable, Boolean defaultProcess,Boolean rejectParentProcess,Boolean rejectGatewayBefore) {
        this.processDefinitionId = processDefinitionId;
        this.processDefinitionKey = processDefinitionKey;
        this.processDefinitionName = processDefinitionName;
        this.version = version;
        this.description = description;
        this.suspended = suspended;
        this.createUser = createUser;
        this.updateUser = updateUser;
        this.createTime = createTime;
        this.updateTime = updateTime;
        this.businessType = businessType;
        this.departmentId = departmentId;
        this.unitId = unitId;
        this.deploymentId = deploymentId;
        this.callable = callable;
        this.defaultProcess = defaultProcess;
        this.rejectParentProcess = rejectParentProcess;
        this.rejectGatewayBefore = rejectGatewayBefore;
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

    public String getCreateUser() {
        return createUser;
    }

    public void setCreateUser(String createUser) {
        this.createUser = createUser;
    }

    public String getUpdateUser() {
        return updateUser;
    }

    public void setUpdateUser(String updateUser) {
        this.updateUser = updateUser;
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

    public Boolean getRejectParentProcess() {
        return rejectParentProcess;
    }

    public void setRejectParentProcess(Boolean rejectParentProcess) {
        this.rejectParentProcess = rejectParentProcess;
    }

    public Boolean getRejectGatewayBefore() {
        return rejectGatewayBefore;
    }

    public void setRejectGatewayBefore(Boolean rejectGatewayBefore) {
        this.rejectGatewayBefore = rejectGatewayBefore;
    }


}
