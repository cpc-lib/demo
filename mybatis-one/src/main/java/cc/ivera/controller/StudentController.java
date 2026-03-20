package cc.ivera.controller;

import cc.ivera.bean.Student;
import cc.ivera.service.StudentService;
import cc.ivera.service.impl.StudentServiceImpl;
import org.junit.Test;

import java.util.List;
import java.util.UUID;

/*
    控制层测试类
 */
public class StudentController {
    //创建业务层对象
    private StudentService service = new StudentServiceImpl();

    //查询全部功能测试
    @Test
    public void selectAll() {
        List<Student> students = service.selectAll();
        for (Student stu : students) {
            System.out.println(stu);
        }
    }

    //根据id查询功能测试
    @Test
    public void selectById() {
        String uuid = UUID.randomUUID().toString();
        Student stu = new Student(uuid,"李世民",27);
        service.insert(stu);
        System.out.println(service.selectById(stu.getId()));
    }

    //新增功能测试
    @Test
    public void insert() {
        String uuid = UUID.randomUUID().toString();
        Student stu = new Student(uuid,"王五",27);
        //Student stu = new Student(4,"赵六",26);
        Integer result = service.insert(stu);
        System.out.println(result);
        String id = stu.getId();
        System.out.println("获取插入后的主键id:"+id);
    }

    //修改功能测试
    @Test
    //TODO 添加事务
    public void update() {
        String uuid = UUID.randomUUID().toString();
        Student stu = new Student(uuid,"王五",29);
        service.insert(stu);
        Integer result = service.update(new Student(stu.getId(),"王麻子",16));
        System.out.println(result);
    }

    //删除功能测试
    @Test
    public void delete() {
        String uuid = UUID.randomUUID().toString();
        Student stu = new Student(uuid,"王五",29);
        service.insert(stu);
        Integer result = service.delete(stu.getId());
        System.out.println(result);
    }
}
