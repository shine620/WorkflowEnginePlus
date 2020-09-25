package com.hy.workflow.repository;

import com.hy.workflow.entity.RejectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RejectRecordRepository extends JpaRepository<RejectRecord, String> {

    void deleteByProcessInstanceId(String processInstanceId);

    void deleteByProcessInstanceIdIn(List<String> processInstanceIds);

}
