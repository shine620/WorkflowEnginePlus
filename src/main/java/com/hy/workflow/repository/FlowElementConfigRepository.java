package com.hy.workflow.repository;

import com.hy.workflow.entity.FlowElementConfig;
import com.hy.workflow.entity.ProcessDefinitionConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowElementConfigRepository extends JpaRepository<FlowElementConfig, String> {

    List<FlowElementConfig> findByProcessDefinitionName(String processDefinitionName);

    List<FlowElementConfig> findFlowElementConfigsByProcessDefinitionId(String processDefinitionName);

    FlowElementConfig findByProcessDefinitionIdAndFlowElementId(String processDefinitionId,String flowElementId);

    void deleteByFlowElementId(String processDefinitionId);

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessDefinitionIdIn(String[] processDefinitionIdIs);


}
