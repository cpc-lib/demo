package cc.ivera;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import cc.ivera.service.FaceEngineService;

import java.io.File;

@SpringBootTest
@RunWith(SpringJUnit4ClassRunner.class)
public class FaceEngineServiceTest {
    @Autowired
    private FaceEngineService faceEngineService;

    @Test
    public void testCheckIsPortrait() {
        File file = new File("D:\\develop\\code\\cs-java\\springboot-face\\springboot-face\\src\\test\\resources\\Faker.jpeg");
        boolean checkIsPortrait = faceEngineService.checkIsPortrait(file);
        System.out.println(checkIsPortrait);
    }
}