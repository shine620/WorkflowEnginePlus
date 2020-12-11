package com.hy.workflow.repository;

import com.hy.workflow.entity.FlowableModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlowableModelRepository extends JpaRepository<FlowableModel, String>, JpaSpecificationExecutor<FlowableModel> {

    FlowableModel findByModelKey(String key);

    void deleteByModelKey(String key);

    void deleteByIdIn(String[] ids);


}
