package com.hy.workflow.repository;

import com.hy.workflow.entity.BusinessProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, String>, JpaSpecificationExecutor<BusinessProcess> {


    void deleteByProcessDefinitionId(String processDefinitionId);

}
