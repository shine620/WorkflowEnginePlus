package com.hy.workflow;

import com.hy.workflow.base.SpringContextUtil;
import com.hy.workflow.config.SwaggerConfig;
import com.hy.workflow.config.ApplicationConfiguration;
import com.hy.workflow.service.ProcessListener;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.spring.SpringProcessEngineConfiguration;
import org.flowable.spring.boot.EngineConfigurationConfigurer;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


//启用全局异常拦截器
@Import(value={ApplicationConfiguration.class,SwaggerConfig.class}) // 引入修改的配置
@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) // 移除 Security 自动配置
public class Application {


    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public CommandLineRunner init(final RepositoryService repositoryService, final RuntimeService runtimeService, final TaskService taskService) {
        return new CommandLineRunner() {
            public void run(String... strings) throws Exception {
                System.out.println("Number of process definitions : " + repositoryService.createProcessDefinitionQuery().count());
                System.out.println("Number of tasks : " + taskService.createTaskQuery().count());
                //runtimeService.startProcessInstanceByKey("oneTaskProcess");
                System.out.println("Number of tasks after process start: " + taskService.createTaskQuery().count());
            }
        };
    }

    @Bean
    public EngineConfigurationConfigurer<SpringProcessEngineConfiguration> customIdGeneratorConfigurer() {
        return  new EngineConfigurationConfigurer<SpringProcessEngineConfiguration>() {
            public void configure(SpringProcessEngineConfiguration engineConfiguration) {
                engineConfiguration.setActivityFontName("宋体");
                engineConfiguration.setLabelFontName("宋体");
                engineConfiguration.setAnnotationFontName("宋体");
                //ID生成策略，使用数值而非UUID
                engineConfiguration.setIdGenerator(new DbIdGenerator());
                //注册事件监听器（ 也可以用流程模型XML添加监听属性 flowable:eventListener ）
                Map<String, List<FlowableEventListener>> typedListeners = new HashMap<String, List<FlowableEventListener>>();
                List<FlowableEventListener> processCompleteList = new ArrayList<FlowableEventListener>();
                ProcessListener processListener = SpringContextUtil.getBeanByClass(ProcessListener.class);
                processCompleteList.add(processListener);
                //processCompleteList.add(new ProcessListener());
                typedListeners.put("PROCESS_COMPLETED", processCompleteList);
                typedListeners.put("PROCESS_STARTED", processCompleteList);
                typedListeners.put("TASK_CREATED", processCompleteList);
                typedListeners.put("TASK_COMPLETED", processCompleteList);
                typedListeners.put("HISTORIC_PROCESS_INSTANCE_ENDED", processCompleteList);
                typedListeners.put("MULTI_INSTANCE_ACTIVITY_STARTED", processCompleteList);
                engineConfiguration.setTypedEventListeners(typedListeners);
            }
        };
    }


}
