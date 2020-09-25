package com.hy.workflow.repository;

import com.hy.workflow.entity.TaskRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TaskRecordRepository extends JpaRepository<TaskRecord, String>, JpaSpecificationExecutor<TaskRecord> {

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessInstanceId(String processInstanceId);

    void deleteByProcessInstanceIdIn(Collection<String> processInstanceIds);

    List<TaskRecord> findByProcessInstanceIdAndTaskDefinitionKeyAndTaskTypeOrderByEndTimeDesc(String processInstanceId,String taskDefinitionKey,String taskType);

}
