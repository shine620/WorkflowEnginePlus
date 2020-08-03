package com.hy.workflow.repository;

import com.hy.workflow.entity.ProcessDefinitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessDefinitionConfigRepository extends JpaRepository<ProcessDefinitionConfig, Long> {

    ProcessDefinitionConfigRepository findByProcessDefinitionName(String processDefinitionName);

}
