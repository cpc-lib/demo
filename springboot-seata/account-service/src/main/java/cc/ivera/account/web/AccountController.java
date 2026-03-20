package cc.ivera.account.web;

import cc.ivera.account.service.AccountService;
import cc.ivera.account.service.AccountTCCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author 虎哥
 */
@RestController
@RequestMapping("account")
public class AccountController {

    @Autowired
    private AccountService accountService;


    @Autowired
    private AccountTCCService accountTCCService;

    @PutMapping("/{userId}/{money}")
    public ResponseEntity<Void> deduct(@PathVariable("userId") String userId, @PathVariable("money") Integer money) {
        //accountService.deduct(userId, money);//xa&at
        accountTCCService.deduct(userId, money);//tcc
        return ResponseEntity.noContent().build();
    }
}
