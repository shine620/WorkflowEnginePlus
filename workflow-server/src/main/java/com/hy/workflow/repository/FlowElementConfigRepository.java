package com.hy.workflow.repository;

import com.hy.workflow.entity.FlowElementConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface FlowElementConfigRepository extends JpaRepository<FlowElementConfig, String> {

    List<FlowElementConfig> findByFlowElementIdIn(Collection<String> flowElementIds);

    FlowElementConfig findByProcessDefinitionIdAndFlowElementId(String processDefinitionId,String flowElementId);

    void deleteByProcessDefinitionId(String processDefinitionId);

    void deleteByProcessDefinitionIdIn(String[] processDefinitionIdIs);


}
