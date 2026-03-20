package top.arhi.wxpush.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Description: TODO
 */
public interface SendService {
    String sendWeChatMsg();
    String messageHandle(HttpServletRequest request, HttpServletResponse response);
}
