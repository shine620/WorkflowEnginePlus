package com.hy.workflow.repository;

import com.hy.workflow.entity.RejectRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RejectRecordRepository extends JpaRepository<RejectRecord, String> {



}
