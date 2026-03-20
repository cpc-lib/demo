package cc.ivera.service.impl;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import cc.ivera.service.AccessService;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;

@Service
public class AccessServiceImpl implements AccessService {


    @Override
    public boolean hasPermission(HttpServletRequest request, Authentication authentication) {

        Object principal = authentication.getPrincipal();
        if(principal instanceof UserDetails){
            UserDetails userDetails = (UserDetails) principal;
            Collection<? extends GrantedAuthority> authorities =
                    userDetails.getAuthorities();

            //判断请求是否在权限集合当中
            return authorities.contains(
                    new SimpleGrantedAuthority(request.getRequestURI()));

        }

        return false;

    }
}
