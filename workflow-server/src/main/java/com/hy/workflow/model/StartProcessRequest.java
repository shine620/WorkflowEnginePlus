package com.hy.workflow.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(value="StartProcessRequest对象",description="封装流程发起请求参数")
public class StartProcessRequest extends ApproveInfo{

    @ApiModelProperty(value="流程定义ID",required = true, example="Model100:2:120052")
    private String processDefinitionId;

    @ApiModelProperty(value="流程发起人ID",required = true,example="zhangsan")
    private String startUserId;

    @ApiModelProperty(value="业务ID",required = true, example="1000")
    private String businessId;

    @ApiModelProperty(value="业务类型",required = true, example="CONTRACT")
    private String businessType;

    @ApiModelProperty(value="业务名称",required = true, example="房屋租赁合同")
    private String businessName;

    @ApiModelProperty(value="业务URL地址",example="las/viewContract/1000")
    private String businessUrl;


    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getStartUserId() {
        return startUserId;
    }

    public void setStartUserId(String startUserId) {
        this.startUserId = startUserId;
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

    public String getBusinessUrl() {
        return businessUrl;
    }

    public void setBusinessUrl(String businessUrl) {
        this.businessUrl = businessUrl;
    }



}
