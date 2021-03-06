package com.hy.workflow.entity;


import com.hy.workflow.common.base.BaseEntity;
import com.hy.workflow.model.ProcessDefinitionConfigModel;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
public class ProcessDefinitionConfig  extends BaseEntity {

    @Id
    //流程定义ID
    private String processDefinitionId;

    //流程定义Key
    private String processDefinitionKey;

    //流程定义名称
    private String processDefinitionName;

    //流程定义版本
    private Integer version;

    @Column(length = 2000)
    //描述信息
    private String description;

    //流程定义挂起状态
    private Boolean suspended;

    //创建人
    private String createUser;

    //修改人
    private String updateUser;

    //业务类型
    private String businessType;

    //部门ID
    private String departmentId;

    //单位ID
    private String unitId;

    //流程部署ID
    private String deploymentId;

    //是子流程
    private Boolean callable;

    //是否默认流程
    private Boolean defaultProcess;

    //允许子流程驳回到主流程
    private Boolean rejectParentProcess;

    //允许驳回到网关发起前
    private Boolean rejectGatewayBefore;

    public ProcessDefinitionConfig(){ };

    public ProcessDefinitionConfig(ProcessDefinitionConfigModel pdcModel) {
        if(pdcModel==null) return;
        this.processDefinitionId = pdcModel.getProcessDefinitionId();
        this.processDefinitionKey = pdcModel.getProcessDefinitionKey();
        this.processDefinitionName = pdcModel.getProcessDefinitionName();
        this.version = pdcModel.getVersion();
        this.description = pdcModel.getDescription();
        this.suspended = pdcModel.getSuspended();
        this.createUser = pdcModel.getCreateUser();
        this.updateUser = pdcModel.getUpdateUser();
        this.businessType = pdcModel.getBusinessType();
        this.departmentId = pdcModel.getDepartmentId();
        this.unitId = pdcModel.getUnitId();
        this.deploymentId = pdcModel.getDeploymentId();
        this.callable = pdcModel.getCallable();
        this.defaultProcess = pdcModel.getDefaultProcess();
        super.setCreateTime(pdcModel.getCreateTime());
        super.setUpdateTime(pdcModel.getUpdateTime());
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
