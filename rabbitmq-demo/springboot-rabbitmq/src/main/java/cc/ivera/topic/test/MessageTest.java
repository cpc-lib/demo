package cc.ivera.topic.test;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import cc.ivera.topic.mq.Provider;


@RunWith(SpringRunner.class)
@SpringBootTest
public class MessageTest {

	@Autowired
	private Provider provider;

	@Test
	public void test() throws Exception {
		provider.send();
	}

}
