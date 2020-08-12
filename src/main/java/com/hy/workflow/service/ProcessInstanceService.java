package com.hy.workflow.service;

import com.hy.workflow.repository.BusinessProcessRepository;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.Map;

@Service
@Transactional
public class ProcessInstanceService {

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private BusinessProcessRepository businessProcessRepository;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private HistoryService historyService;


    public ProcessInstance startProcess(ProcessDefinition processDefinition, String businessId, String businessType, String businessName, Map<String,Object> variables) {
        Authentication.setAuthenticatedUserId("zhaoyao");
        ProcessInstanceBuilder processInstanceBuilder = runtimeService.createProcessInstanceBuilder()
                .processDefinitionId(processDefinition.getId())
                .businessKey(businessType+";"+businessId)
                .name( businessName==null?"":businessName  +"-" +processDefinition.getName() )
                .variables(variables);
        ProcessInstance instance = processInstanceBuilder.start();
        // 这个方法最终使用一个ThreadLocal类型的变量进行存储，也就是与当前的线程绑定，所以流程实例启动完毕之后，需要设置为null，防止多线程的时候出问题
        Authentication.setAuthenticatedUserId(null);
        return instance;
    }


    public void deleteProcessInstance(String processInstanceId, String deleteReason) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().processInstanceId(processInstanceId).singleResult();
        if(processInstance==null) throw new FlowableObjectNotFoundException("流程实例不存在：" + processInstanceId );
        runtimeService.deleteProcessInstance(processInstanceId,deleteReason);
        //删除BusinessProcess表中信息

    }


}
