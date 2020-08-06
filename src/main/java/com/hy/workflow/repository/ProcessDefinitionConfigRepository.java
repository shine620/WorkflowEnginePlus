package com.hy.workflow.repository;

import com.hy.workflow.entity.ProcessDefinitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProcessDefinitionConfigRepository extends JpaRepository<ProcessDefinitionConfig, String> {

    List<ProcessDefinitionConfig> findByProcessDefinitionName(String processDefinitionName);

    ProcessDefinitionConfig findByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessDefinitionIdIn(String[] processDefinitionIdIs);


}
