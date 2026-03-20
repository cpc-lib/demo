package cc.ivera.dtx.seatademo.bank1.controller;

import cc.ivera.dtx.seatademo.bank1.service.AccountInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class Bank1Controller {
    @Autowired
    AccountInfoService accountInfoService;

    //转账
    @GetMapping("/transfer")
    public String transfer(@RequestParam("amount") Double amount) {
        accountInfoService.updateAccountBalance("1", amount);
        return "bank1:" + amount;
    }
}