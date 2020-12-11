package com.hy.workflow.model;


public class FlowElementModel {

    //节点ID
    private String id;

    //节点名称
    private String name;

    //节点类型
    private String flowElementType;

    //内部子流程节点时对应的子流程容器ID
    private String parentId;

    //部门ID
    private String  departmentId;

    //模型Key
    private String  modelKey;

    //模型名称
    private String  modelName;

    //节点配置
    private FlowElementConfigModel  config;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlowElementType() {
        return flowElementType;
    }

    public void setFlowElementType(String flowElementType) {
        this.flowElementType = flowElementType;
    }

    public String getParentId() {
        return parentId;
    }

    public void setParentId(String parentId) {
        this.parentId = parentId;
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
