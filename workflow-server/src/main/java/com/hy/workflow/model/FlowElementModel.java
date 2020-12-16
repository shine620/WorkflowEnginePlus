package com.hy.workflow.model;


public class FlowElementModel {

    //节点ID
    private String flowElementId;

    //节点名称
    private String flowElementName;

    //节点类型
    private String flowElementType;

    //内外部子流程节点时对应的子流程容器ID或者调用活动节点ID
    private String parentFlowElementId ;

    //内部子流程或者外部子流程
    private String parentFlowElementType;

    //部门ID(外部子流程时的发起部门)
    private String  departmentId;

    //模型Key
    private String  modelKey;

    //模型名称
    private String  modelName;

    //节点配置
    private FlowElementConfigModel  config;


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

    public String getParentFlowElementId() {
        return parentFlowElementId;
    }

    public void setParentFlowElementId(String parentFlowElementId) {
        this.parentFlowElementId = parentFlowElementId;
    }

    public String getParentFlowElementType() {
        return parentFlowElementType;
    }

    public void setParentFlowElementType(String parentFlowElementType) {
        this.parentFlowElementType = parentFlowElementType;
    }

    public String getFlowElementType() {
        return flowElementType;
    }

    public void setFlowElementType(String flowElementType) {
        this.flowElementType = flowElementType;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getModelKey() {
        return modelKey;
    }

    public void setModelKey(String modelKey) {
        this.modelKey = modelKey;
    }

    public FlowElementConfigModel getConfig() {
        return config;
    }

    public void setConfig(FlowElementConfigModel config) {
        this.config = config;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

}
