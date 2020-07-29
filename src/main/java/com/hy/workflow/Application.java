package com.hy.workflow;

import com.hy.workflow.config.SwaggerConfig;
import com.hy.workflow.config.ApplicationConfiguration;
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

//启用全局异常拦截器
@Import(value={
        // 引入修改的配置
        ApplicationConfiguration.class,
        SwaggerConfig.class})

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class}) // 移除 Security 自动配置
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }


    @Bean
    public CommandLineRunner init(final RepositoryService repositoryService, final RuntimeService runtimeService, final TaskService taskService) {
        return new CommandLineRunner() {
            // @Override
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
                engineConfiguration.setIdGenerator(new DbIdGenerator());
            }
        };
    }

}
