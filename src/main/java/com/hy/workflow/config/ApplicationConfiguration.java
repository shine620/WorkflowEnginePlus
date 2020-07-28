/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hy.workflow.config;

import org.flowable.ui.common.rest.idm.remote.RemoteAccountResource;
import org.flowable.ui.common.service.idm.RemoteIdmService;
import org.flowable.ui.common.service.idm.RemoteIdmServiceImpl;
import org.flowable.ui.modeler.conf.SecurityConfiguration;
import org.flowable.ui.modeler.properties.FlowableModelerAppProperties;
import org.flowable.ui.modeler.rest.app.EditorGroupsResource;
import org.flowable.ui.modeler.rest.app.EditorUsersResource;
import org.flowable.ui.modeler.security.RemoteIdmAuthenticationProvider;
import org.flowable.ui.modeler.servlet.ApiDispatcherServletConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.DispatcherServlet;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(FlowableModelerAppProperties.class)
@ComponentScan(
        basePackages = {
                "org.flowable.ui.modeler.conf",
                "org.flowable.ui.modeler.repository",
                "org.flowable.ui.modeler.service",
                "org.flowable.ui.modeler.security",
                "org.flowable.ui.common.conf",
                "org.flowable.ui.common.filter",
                "org.flowable.ui.common.service",
                "org.flowable.ui.common.repository",
                "org.flowable.ui.common.security",
                "org.flowable.ui.common.tenant",
                "org.flowable.ui.modeler.rest.app",
                "org.flowable.ui.common.rest"
        },

        excludeFilters = {
                //移除flowable.cmmon.app 的设置
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = EditorUsersResource.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = EditorGroupsResource.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = RemoteIdmServiceImpl.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = RemoteIdmAuthenticationProvider.class),
                //移除flowable 中的spring security 的设置
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.ApiWebSecurityConfigurationAdapter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.ActuatorWebSecurityConfigurationAdapter.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = SecurityConfiguration.FormLoginWebSecurityConfigurerAdapter.class),
                //编辑器国际化文件 这个在flowable 6.5 版本中前端支持国际化了， 不需要排除了
                //@ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,classes = StencilSetResource.class),
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = org.flowable.ui.modeler.conf.ApplicationConfiguration.class) ,
                // 排除获取用户信息，采用自定义方式实现
                @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = RemoteAccountResource.class)
        })

public class ApplicationConfiguration {

    @Bean
    public ServletRegistrationBean modelerApiServlet(ApplicationContext applicationContext) {
        AnnotationConfigWebApplicationContext dispatcherServletConfiguration = new AnnotationConfigWebApplicationContext();
        dispatcherServletConfiguration.setParent(applicationContext);
        dispatcherServletConfiguration.register(ApiDispatcherServletConfiguration.class);
        DispatcherServlet servlet = new DispatcherServlet(dispatcherServletConfiguration);
        ServletRegistrationBean registrationBean = new ServletRegistrationBean(servlet, "/api/*");
        registrationBean.setName("Flowable Modeler App API Servlet");
        registrationBean.setLoadOnStartup(1);
        registrationBean.setAsyncSupported(true);
        return registrationBean;
    }

}
