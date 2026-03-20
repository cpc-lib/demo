package cc.ivera.domain;

import java.util.List;

public class Student {
    private String id;     //主键id
    private String name;    //学生姓名
    private String age;    //学生年龄

    private List<Course> courses;   // 学生所选择的课程集合

    public Student() {
    }

    public Student(String id, String name, String age, List<Course> courses) {
        this.id = id;
        this.name = name;
        this.age = age;
        this.courses = courses;
    }

    public List<Course> getCourses() {
        return courses;
    }

    public void setCourses(List<Course> courses) {
        this.courses = courses;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    /*仅打印学生id，age，name信息*/
    @Override
    public String toString() {
        return "Student{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", age=" + age +
                '}';
    }
}
