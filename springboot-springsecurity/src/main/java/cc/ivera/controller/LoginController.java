package cc.ivera.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class LoginController {

//    @RequestMapping(value="/login", method= RequestMethod.POST)
//    public String login(){
//        return "redirect:main.html";
//    }

    //使用rememberme注解，可实现免登，spring security自动存储数据到数据源中，rememberme依赖spring-jdbc。
    //使用需要导入配置依赖
    //仅用于此角色的用户可以访问
    //@Secured("ROLE_abcd")
    //PreAuthorize可以以'ROLE_'开头
    @PreAuthorize("hasRole('ROLE_abc')")
    @RequestMapping(value="/toIndex",method=RequestMethod.POST)
    public String index(HttpServletRequest request){
        if(Boolean.parseBoolean(request.getParameter("remember-me"))){
            System.out.println("checked");
        }
        return "redirect:main.html";
    }

    @RequestMapping(value="/toFail",method=RequestMethod.POST)
    public String fail(){
        return "redirect:fail.html";
    }


    @RequestMapping(value="/demo",method=RequestMethod.GET)
    @ResponseBody
    public String demo(){
        return "demo2";
    }

//    @RequestMapping(value="/mvc/demo1",method=RequestMethod.GET)
//    @ResponseBody
//    public String demo1(){
//        return "demo3";
//    }


    @RequestMapping(value="/showLogin",method=RequestMethod.GET)
    public String showLogin(){
        return "login";
    }
}
