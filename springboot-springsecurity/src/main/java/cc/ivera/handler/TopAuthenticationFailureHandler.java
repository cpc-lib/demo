package cc.ivera.handler;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class TopAuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String forwardUrl;

    public TopAuthenticationFailureHandler(String forwardUrl) {
        this.forwardUrl = forwardUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, AuthenticationException e) throws IOException, ServletException {
        String remoteAddr = httpServletRequest.getRemoteAddr();
        System.out.println(remoteAddr);
        httpServletResponse.sendRedirect(forwardUrl);
    }
}
