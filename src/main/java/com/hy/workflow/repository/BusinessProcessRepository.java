package com.hy.workflow.repository;

import com.hy.workflow.entity.BusinessProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
public interface BusinessProcessRepository extends JpaRepository<BusinessProcess, String>, JpaSpecificationExecutor<BusinessProcess> {


    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessInstanceIdIn(Collection<String> processInstanceIds);

}
