package cc.ivera.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import cc.ivera.config.SpringConfig;
import cc.ivera.service.AccountService;

import java.io.IOException;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes= SpringConfig.class)
public class TransactionTest {
    @Autowired
    private AccountService accountService;

    @Test
    public void testTransaction() throws IOException {
        accountService.transfter("sun","ming",1000);
    }

}
