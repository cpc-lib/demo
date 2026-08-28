package cc.ivera.dtx.tccdemo.bank2.controller;

import cc.ivera.dtx.tccdemo.bank2.service.AccountInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Bank2Controller {
    @Autowired
    AccountInfoService accountInfoService;

    @RequestMapping("/transfer")
    public Boolean test2(@RequestParam("amount") Double amount) {
        if (amount.equals(new Double(2.0D))) {
            return false;
        }
        this.accountInfoService.updateAccountBalance("2", amount);
        return true;
    }
}