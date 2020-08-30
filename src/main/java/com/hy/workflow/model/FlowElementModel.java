package com.hy.workflow.model;


public class FlowElementModel {

    //节点ID
    private String id;

    //节点名称
    private String name;

    //节点类型
    private String flowElementType;

    //子流程节点时对应的主流程节点ID
    private String parentId;

    //子流程节点时对应的主流程节点名称
    private String parentName;

    //子流程节点时对应的主流程节点类型
    private String parentType;

    //部门ID
    private String  departmentId;

    //模型Key
    private String  modelKey;

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

    public String getParentName() {
        return parentName;
    }

    public void setParentName(String parentName) {
        this.parentName = parentName;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getParentType() {
        return parentType;
    }

    public void setParentType(String parentType) {
        this.parentType = parentType;
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


}
