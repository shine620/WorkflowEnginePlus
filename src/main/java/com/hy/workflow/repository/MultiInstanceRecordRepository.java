package com.hy.workflow.repository;

import com.hy.workflow.entity.MultiInstanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface MultiInstanceRecordRepository  extends JpaRepository<MultiInstanceRecord, String>, JpaSpecificationExecutor<MultiInstanceRecord> {

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessInstanceId(String processInstanceId);

    void deleteByProcessInstanceIdIn(List<String> processInstanceIds);

    //查找子流程/调用活动会签记录
    List<MultiInstanceRecord> findByProcessInstanceIdAndActivityIdAndSubProcessDefinitionKeyListIsNotNullOrderByCreateTimeDesc(String processInstanceId, String activityId);

    //查找用户会签记录
    List<MultiInstanceRecord> findByProcessInstanceIdAndActivityIdAndAssigneeListIsNotNullOrderByCreateTimeDesc(String processInstanceId, String activityId);


}
