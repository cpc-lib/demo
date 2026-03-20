package cc.ivera;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

@SpringBootTest
@RunWith(SpringRunner.class)
public class RestTemplateTest {

    @Autowired
    private RestTemplate restTemplate;

    @Test
    public void findById(){
        //User forObject = restTemplate.getForObject("http://localhost:8080/user/{id}", User.class,4);
        //System.out.println(forObject);
        /*
        *resTemplate的使用，controller返回的数据为json字符串，RestTemplate中的字节码可写为String.class。
        * */
        String userJson = restTemplate.getForObject("http://localhost:8080/user/{id}", String.class, 4);
        System.out.println(userJson);
    }
}
