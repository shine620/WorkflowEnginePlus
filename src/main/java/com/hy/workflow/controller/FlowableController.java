package com.hy.workflow.controller;

import org.flowable.ui.common.model.RemoteUser;
import org.flowable.ui.common.model.UserRepresentation;
import org.flowable.ui.common.security.DefaultPrivileges;
import org.flowable.ui.common.security.SecurityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;

@ApiIgnore
@Controller
//@RequestMapping("/app")
public class FlowableController {


    @RequestMapping("/")
    public String index(HttpServletResponse response) {
        return "forward:index.html";
    }

    /*@RequestMapping("/")
    public String index(HttpServletResponse response) {
        return "forward:flowable.html";
    }*/

    /*@Controller
    public class IndexController {
        @RequestMapping("/")
        public String homePage(){
            return "index";
        }
    }*/

    /**
     * 获取默认的管理员信息
     * @return
     */
    /*@RequestMapping(value = "/rest/account", method = RequestMethod.GET, produces = "application/json")
    public UserRepresentation getAccount() {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setId("zhaoyao");
        userRepresentation.setEmail("zhaoyao_620@flowable.org");
        userRepresentation.setFullName("赵耀");
        userRepresentation.setLastName("耀");
        userRepresentation.setFirstName("赵");
        List<String> privileges = new ArrayList<>();
        privileges.add(DefaultPrivileges.ACCESS_MODELER);
        privileges.add(DefaultPrivileges.ACCESS_IDM);
        privileges.add(DefaultPrivileges.ACCESS_ADMIN);
        privileges.add(DefaultPrivileges.ACCESS_TASK);
        privileges.add(DefaultPrivileges.ACCESS_REST_API);
        userRepresentation.setPrivileges(privileges);
        return userRepresentation;
    }*/

    @GetMapping(value = "/app/rest/account", produces = "application/json")
    @ResponseBody
    public UserRepresentation getAccount() {
        RemoteUser remoteUser= new RemoteUser();
        remoteUser.setFirstName("赵");
        remoteUser.setLastName("耀");
        remoteUser.setFullName("赵耀");
        remoteUser.setEmail("123456@qq.com");
        remoteUser.setId("zhaoyao");
        //构建用户代表类
        UserRepresentation userRepresentation = new UserRepresentation(remoteUser);
        SecurityUtils.assumeUser(remoteUser); //保证创建流程可用
        return userRepresentation;
    }




}
