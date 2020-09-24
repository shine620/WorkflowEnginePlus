package com.hy.workflow.entity;

import org.hibernate.annotations.GenericGenerator;
import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;


@Entity
@Table
public class MultiInstanceRecord implements Serializable {

    @Id
    @GenericGenerator(name="idGenerator", strategy="uuid")
    @GeneratedValue(generator="idGenerator")
    private String id;

    private String processInstanceId;

    private String processDefinitionId;

    private String activityId;

    private String activityName;

    private String activityType;

    private Date createTime;

    @Column(length = 2000)
    private String assigneeList;

    @Column(length = 2000)
    private String subProcessDefinitionKeyList;

    @Column(length = 4000)
    private String callActivityList;


    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessDefinitionId() {
        return processDefinitionId;
    }

    public void setProcessDefinitionId(String processDefinitionId) {
        this.processDefinitionId = processDefinitionId;
    }

    public String getActivityId() {
        return activityId;
    }

    public void setActivityId(String activityId) {
        this.activityId = activityId;
    }

    public String getActivityName() {
        return activityName;
    }

    public void setActivityName(String activityName) {
        this.activityName = activityName;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public String getAssigneeList() {
        return assigneeList;
    }

    public void setAssigneeList(String assigneeList) {
        this.assigneeList = assigneeList;
    }

    public String getSubProcessDefinitionKeyList() {
        return subProcessDefinitionKeyList;
    }

    public void setSubProcessDefinitionKeyList(String subProcessDefinitionKeyList) {
        this.subProcessDefinitionKeyList = subProcessDefinitionKeyList;
    }

    public String getCallActivityList() {
        return callActivityList;
    }

    public void setCallActivityList(String callActivityList) {
        this.callActivityList = callActivityList;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

}
