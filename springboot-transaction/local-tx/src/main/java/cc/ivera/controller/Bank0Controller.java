package cc.ivera.controller;

import cc.ivera.service.AccountInfoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@RestController
public class Bank0Controller {

    @Resource
    private AccountInfoService accountInfoService;

    @GetMapping("/transfer")
    public void transfer(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Boolean flag = accountInfoService.updateAccountBalance();
        response.setContentType("application/json;charset=utf-8");
        PrintWriter writer = response.getWriter();
        if(flag){
            writer.write("转账成功");
        }else{
            writer.write("转账失败");
        }


    }

}
