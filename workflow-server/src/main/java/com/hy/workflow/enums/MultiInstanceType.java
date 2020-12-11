package com.hy.workflow.enums;

public enum MultiInstanceType {


    NONE("NONE","无"),
    PARALLEL("PARALLEL","并行会签"),
    SEQUENTIAL("SEQUENTIAL","串行会签");

    private String type;
    private String name;

    MultiInstanceType(String type,String name){
        this.type = type;
        this.name = name;
    }

    public String getType(){
        return this.type;
    }

    public String getName(){
        return this.name;
    }


}
