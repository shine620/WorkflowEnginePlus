package com.hy.workflow.service;


import com.hy.workflow.common.base.BaseRequest;
import com.hy.workflow.common.base.PageBean;
import com.hy.workflow.common.base.WorkflowException;
import com.hy.workflow.entity.BusinessProcess;
import com.hy.workflow.entity.BusinessType;
import com.hy.workflow.entity.FlowableModel;
import com.hy.workflow.repository.BusinessProcessRepository;
import com.hy.workflow.repository.BusinessTypeRepository;
import org.apache.commons.lang3.StringUtils;
import org.flowable.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 流程引擎Service
 * @author zhoayao
 * @version 1.0
 *
 */
@Service
@Transactional
public class WorkflowService {

    @Autowired
    protected ManagementService managementService;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;

    @Autowired
    protected BusinessTypeRepository businessTypeRepository;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    /**
     * 获取业务类型
     *
     * @author:  zhaoyao
     * @param  baseRequest 参数封装
     * @param  pageRequest 分页参数
     * @return PageBean<BusinessType> 业务类型分页数据
     */
    public PageBean<BusinessType> findBusinessType(BaseRequest baseRequest, PageRequest pageRequest) {
        Specification<BusinessType> specification = (Specification<BusinessType>) (root, criteriaQuery, criteriaBuilder) -> {
            //设置查询条件
            List<Predicate> predicatesList = new ArrayList<>();
            if(baseRequest.getFields()!=null){
                Map<String,Object> fields = baseRequest.getFields();
                if (StringUtils.isNotBlank((String)fields.get("name"))) {
                    Predicate predicate = criteriaBuilder.and( criteriaBuilder.like( root.get("name"), "%" + fields.get("name") + "%"));
                    predicatesList.add(predicate);
                }
                if (StringUtils.isNotBlank((String)fields.get("id"))) {
                    predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("id"),  fields.get("id")) )  );
                }
                if (StringUtils.isNotBlank((String)fields.get("code"))) {
                    predicatesList.add(  criteriaBuilder.and( criteriaBuilder.equal( root.get("code"),  fields.get("code")) )  );
                }
            }
            Predicate predicate = criteriaBuilder.and( predicatesList.toArray(new Predicate[predicatesList.size()]) );
            return predicate;
        };
        Page<BusinessType> datas = businessTypeRepository.findAll(specification, pageRequest);
        PageBean page = new PageBean(datas);
        return page;
    }


    /**
     * 删除业务类型
     *
     * @author:  zhaoyao
     * @param:  id  业务类型ID
     */
    public void deleteBusinessType(String id) {
        businessTypeRepository.deleteById(id);
    }


    /**
     * 批量删除业务类型
     *
     * @author:  zhaoyao
     * @param:  ids  业务类型ID
     */
    public void batchDeleteBusinessType(String[] ids) {
        businessTypeRepository.deleteByIdIn(ids);
    }


    /**
     * 保存业务类型
     *
     * @author:  zhaoyao
     * @param:  businessType  业务类型对象
     */
    public BusinessType saveBusinessType(BusinessType businessType) {
        BusinessType bt;
        //新建
        if(StringUtils.isBlank(businessType.getId())){
            businessType.setId(null); //可能为空字符串，设置为null，保存时自动生成ID
            bt = businessTypeRepository.save(businessType);
        }
        //修改
        else{
            Optional<BusinessType> op =  businessTypeRepository.findById(businessType.getId());
            if(op.isPresent()) bt = op.get();
            else throw new WorkflowException("业务类型不存在：id="+businessType.getId());
            bt.setCode(businessType.getCode());
            bt.setName(businessType.getName());
            bt.setDescription(businessType.getDescription());
            businessTypeRepository.save(bt);
        }
        return bt;
    }


    /**
     * 查询业务类型
     *
     * @author:  zhaoyao
     * @param:  id 业务类型ID
     */
    public BusinessType getBusinessType(String id) {
        Optional<BusinessType> op =  businessTypeRepository.findById(id);
        return op.get();
    }


    /**
     * 获取所有业务类型
     *
     * @author:  zhaoyao
     */
    public List<BusinessType> getAllBusinessType() {
        return businessTypeRepository.findAll();
    }


    /**
     * 查找流程业务关联数据
     *
     * @author:  zhaoyao
     * @param:  businessId 业务ID
     * @param:  businessType 业务类型
     */
    public List<BusinessProcess> findBusinessProcess(String businessId,String businessType) {
        return  businessProcessRepository.findAllByBusinessIdAndBusinessType(businessId,businessType);
    }



}
