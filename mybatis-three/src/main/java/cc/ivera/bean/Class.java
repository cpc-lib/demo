package cc.ivera.bean;

import java.util.List;

public class Class {
    /**
     * 主键id
     */
    private String id;
    /**
     * 班级名称
     */
    private String name;

    /**
     * 班级中所有学生对象
     */
    private List<Student> students;

    public Class() {
    }

    public Class(String id, String name, List<Student> students) {
        this.id = id;
        this.name = name;
        this.students = students;
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

    public List<Student> getStudents() {
        return students;
    }

    public void setStudents(List<Student> students) {
        this.students = students;
    }

    /*@Override
    public String toString() {
        return "Classes{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", students=" + students +
                '}';
    }*/
}
