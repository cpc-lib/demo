package cc.ivera;

import com.github.pagehelper.PageHelper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import cc.ivera.domain.User;
import cc.ivera.mapper.UserMapper;

import java.util.List;

@SpringBootTest(classes=Application.class)
@RunWith(SpringRunner.class)
public class TestA {


    @Autowired
    private UserMapper userMapper;

    @Test
    public void test1(){
        User user = userMapper.selectByPrimaryKey(1);//org.apache.ibatis.binding.MapperProxy@89caf47
        System.out.println(user);
    }

    @Test
    public void test2(){
        PageHelper.startPage(1,1);
        List<User> list=userMapper.selectAll();
        System.out.println(list.size());
    }

    //说明test3与test4单独测试(resultMap，指定type为User, resultType 为 User 导致binding statement)
    @Test
    public void test3(){
        User findById = userMapper.findById(1L);
        System.out.println(findById);
    }

    @Test
    public void test4(){
        User selectById= userMapper.selectById(1L);
        System.out.println(selectById);
    }

    @Test
    public void test6(){
        User user = new User(null,"123456","@12345678a","172.168.12.20","0.0.0.0");
        int i=userMapper.insert(user);
        System.out.println(user.getId());
    }
}
