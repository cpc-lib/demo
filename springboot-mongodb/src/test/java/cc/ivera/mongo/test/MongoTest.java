package cc.ivera.mongo.test;

import cc.ivera.mongo.MongoApplication;
import cc.ivera.mongo.domain.Person;
import org.bson.types.ObjectId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = MongoApplication.class)
public class MongoTest {


    /*
     *    1、配置实体类
     *    2、实体类上配置注解（配置集合和对象间的映射关系）
     *    3、注入MongoTemplate对象
     *    4、调用对象方法，完成数据库操作
     */
    @Autowired
    private MongoTemplate mongoTemplate;

    //保存
    @Test
    public void testSave() {
        for (int i = 0; i < 106; i++) {
            Person person = new Person();
            person.set_id(ObjectId.get()); //ObjectId.get()：获取一个唯一主键字符串
            person.setName("张三" + i);
            person.setAddress("北京顺义" + i);
            person.setAge(18 + i);
            int j = i + 1;
            Long userId = new Long(j);
            person.setUserId(userId);
            mongoTemplate.save(person);
        }
    }

    //查询-查询所有
    @Test
    public void testFindAll() {
        List<Person> list = mongoTemplate.findAll(Person.class);
        for (Person person : list) {
            System.out.println(person);
        }
    }

    @Test
    public void testFind() {
        //查询年龄小于20的所有人
        Query query = new Query(Criteria.where("age").lt(20)); //查询条件对象
        //查询
        List<Person> list = mongoTemplate.find(query, Person.class);

        for (Person person : list) {
            System.out.println(person);
        }
    }

    /**
     * 分页查询
     */
    @Test
    public void testPage() {
        Criteria criteria = Criteria.where("age").lt(30);
        //1、查询总数
        Query queryCount = new Query(criteria);
        //统计查询结果的条数
        long count = mongoTemplate.count(queryCount, Person.class);
        System.out.println(count);
        //2、查询当前页的数据列表, 查询第二页，每页查询2条
        Query queryLimit = new Query(criteria)
                .skip(2)//跳过两条
                .limit(2)//设置每页询条数
                ; //开启查询的条数 （page-1）*size
        long count1 = mongoTemplate.count(queryLimit, Person.class);
        System.out.println(count1);
        List<Person> list = mongoTemplate.find(queryLimit, Person.class);
        for (Person person : list) {
            System.out.println(person);
        }
    }


    /**
     * 更新:
     * 根据id，更新年龄
     */
    @Test
    public void testUpdate() {
        //1、条件
        Query query = Query.query(Criteria.where("_id").is("688ad76cec7c5b0558120fbe"));
        //2、更新的数据
        Update update = new Update();
        update.set("age", 20);
        mongoTemplate.updateFirst(query, update, Person.class);
    }

    @Test
    public void testFindById() {
        //1、条件
        Query query = Query.query(Criteria.where("_id").is(new ObjectId("688ad76cec7c5b0558120fbe")));
        //根据id查询
        Person person = (Person) mongoTemplate.findOne(query, Person.class);
        //System.out.println(null) 编译异常，程序无法启动
        //此处进行了null的判断，对象为null，没有调用null.toString()方法，程序进行处理完成字符串”null“的写入
        //对象为null不能调用方法，否则抛出空指针异常。
        System.out.println(person.toString());//java.lang.NullPointerException
        //System.out.println(person); 程序进行处理完wirte("null")
    }

    @Test
    public void testRemove() {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId("688ad76cec7c5b0558120fbe")));
        //返回的DeleteResult恒为有效值不为null
        if (mongoTemplate.remove(query, Person.class) != null) {
            System.out.println("已被删除");
        }
        //返回的list恒为有效值不为null，但size为0，如果被删除list的size为0
        if (mongoTemplate.find(query, Person.class).size() == 0) {
            System.out.println("不存在此id");
        }
    }

    @Test
    public void testTanhua() {
        Query query = Query.query(Criteria.where("userId").is(106));
        List<Person> people = mongoTemplate.find(query, Person.class);
        if (people.size() == 0) {
            System.out.println("不存在此id");
        } else {
            Person person = people.get(0);
            System.out.println(person);
        }
    }
}
