package com.hy.workflow.repository;

import com.hy.workflow.entity.MultiInstanceRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface MultiInstanceRecordRepository  extends JpaRepository<MultiInstanceRecord, String>, JpaSpecificationExecutor<MultiInstanceRecord> {

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessInstanceId(String processInstanceId);

}
