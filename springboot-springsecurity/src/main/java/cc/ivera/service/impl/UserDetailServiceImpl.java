package cc.ivera.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserDetailServiceImpl implements UserDetailsService {

    @Autowired
    private PasswordEncoder passwordEncoder;


    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {

        System.out.println("自定义登录逻辑");

        //模拟查询数据库
        if(!"admin".equals(s)){
            throw new UsernameNotFoundException("Not Found");
        }
        //注册时的密文(查询数据库的密文),默认加密为10
        //org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder（line 97）
        String password = passwordEncoder.encode("123");


        //admin,normal 权限
        //ROLE_abc 具备角色abc
        // ‘/main.html’ 验证自定义access
        return new User(s,password, AuthorityUtils.
                commaSeparatedStringToAuthorityList("admin,normal,ROLE_abc,/main.html"));
    }





}
