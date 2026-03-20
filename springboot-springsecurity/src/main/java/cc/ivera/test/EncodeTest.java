package cc.ivera.test;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

@SpringBootTest
@RunWith(SpringRunner.class)
public class EncodeTest {


    @Test
    public void testEncode(){
        PasswordEncoder pw = new BCryptPasswordEncoder();
        String encode = pw.encode("hjm9796");
        System.out.println(encode);

        boolean flag = pw.matches("hjm9796", encode);
        System.out.println(flag);


    }
}
