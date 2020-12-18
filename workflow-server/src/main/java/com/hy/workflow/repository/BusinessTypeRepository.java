package com.hy.workflow.repository;

import com.hy.workflow.entity.BusinessType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BusinessTypeRepository extends JpaRepository<BusinessType, String>, JpaSpecificationExecutor<BusinessType> {

    BusinessType findByCode(String key);

    void deleteByCode(String key);

    void deleteByIdIn(String[] ids);


}
