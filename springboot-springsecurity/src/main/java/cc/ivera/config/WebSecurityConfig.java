package cc.ivera.config;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.rememberme.JdbcTokenRepositoryImpl;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;
import cc.ivera.handler.TopAccessDeniedHandler;
import cc.ivera.service.impl.UserDetailServiceImpl;

import javax.sql.DataSource;


@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserDetailServiceImpl userdetails;


    @Override
    protected void configure(HttpSecurity http) throws Exception {
        //设置自定义的登录页面
        http.formLogin()
                //自定义请求参数
//                .usernameParameter("username")
//                .usernameParameter("password")
                //设置自定义的登录页面
                .loginPage("/login.html")

                .loginProcessingUrl("/login")
                .successForwardUrl("/toIndex")
                //登录成功跳转到百度（需要配置successHandler）
                //.successForwardUrl("https://www.baidu.com")
                //成功与失败处理器(实际做法)
                //.successHandler(new TopAuthenticationSuccessHandler("http://www.baidu.com"))
                .failureForwardUrl("/toFail");
                  //      .failureHandler(new TopAuthenticationFailureHandler("https://www.google.com"));

        //授权
        http.authorizeRequests()
                //放行错误页面，不需要认证
                .antMatchers("/fail.html").permitAll()
                //放行自定义登录页面，不需要认证
                //.antMatchers("/login.html").permitAll()
                //放行静态资源
                .antMatchers("/css/**","/js/**","/images/**").permitAll()
                .antMatchers("/**/*.png").permitAll()
                //正则规则放行
                .regexMatchers("/demo").permitAll()
                .antMatchers("/showLogin").permitAll()
                //权限 区分大小写 （403）
                //.antMatchers("/main1.html").hasAuthority("admiN")
                //.antMatchers("/main1.html").hasAnyAuthority("admiN","admin")
                //角色 区分大小写 （403）
                //.antMatchers("/main2.html").access("hasRole(\"abcd\")")
                //开启dingtalk的内网穿透工具，访问者的ip都是127.0.0.1;
                .antMatchers("/main3.html").hasIpAddress("127.0.0.1")
                //.mvcMatchers("/demo1").servletPath("/mvc").permitAll()
                //.anyRequest().authenticated();
                //自定义access方方法缩小认证范围
                .anyRequest().access("@accessServiceImpl.hasPermission(request,authentication)");


        //记住我
        http.rememberMe()
                //设置数据源
                .tokenRepository(persistentTokenRepository())
                //设置记住我60s
                //org.springframework.security.web.authentication.rememberme.AbstractRememberMeServices
                .tokenValiditySeconds(60)
                //设置userdetails
                .userDetailsService(userdetails);

        //403权限不足，处理方案。
        http.exceptionHandling()
                .accessDeniedHandler(new TopAccessDeniedHandler());

        //退出（配置解决退出登录，url路径带参数?logout）
        http.logout()
                //开发一般默认做以下设置（logout的源码阅读 logoutCofigurer）
                .logoutUrl("/logout")
                //退出后跳转到的页面
                .logoutSuccessUrl("/login.html");

        //关闭csrf(跨站请求伪造)

        //进行csrf验证注释下行语句
        //http.csrf().disable();


    }

    @Bean
    public PasswordEncoder getPw(){
        return new BCryptPasswordEncoder();
    }


    @Bean
    public PersistentTokenRepository persistentTokenRepository(){
        JdbcTokenRepositoryImpl tokenRepository = new JdbcTokenRepositoryImpl();
        tokenRepository.setDataSource(dataSource);
        //每次启动自动创建数据表，第二次启动项目需要注释一下语句
        //tokenRepository.setCreateTableOnStartup(true);
        return tokenRepository;
    }
}
