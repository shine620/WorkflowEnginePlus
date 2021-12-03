package com.hy.workflow.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="StartProcessRequest对象",description="封装流程发起请求参数")
public class StartProcessRequest extends ApproveInfo{

    @ApiModelProperty(value="流程定义ID",required = true, example="Model100:2:120052")
    private String processDefinitionId;

    @ApiModelProperty(value="业务ID",required = true, example="1000")
    private String businessId;

    @ApiModelProperty(value="业务类型",required = true, example="CONTRACT")
    private String businessType;

    @ApiModelProperty(value="业务名称",required = true, example="房屋租赁合同")
    private String businessName;

    @ApiModelProperty(value="业务数据编辑地址",example="/EditContract?id=1000")
    private String editUrl;

    @ApiModelProperty(value="业务数据查看地址",example="/ViewContract?id=1000")
    private String viewUrl;


    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
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

    public String getEditUrl() {
        return editUrl;
    }

    public void setEditUrl(String editUrl) {
        this.editUrl = editUrl;
    }

    public String getViewUrl() {
        return viewUrl;
    }

    public void setViewUrl(String viewUrl) {
        this.viewUrl = viewUrl;
    }

}
