package com.hy.workflow.service;


import com.hy.workflow.repository.FlowElementConfigRepository;
import org.flowable.engine.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private RepositoryService repositoryService;

    @Autowired
    protected ManagementService managementService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private FlowElementConfigRepository flowElementConfigRepository;

    @Autowired
    @Qualifier("processEngine")
    protected ProcessEngine engine;



}
