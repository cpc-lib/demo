package cc.ivera.model.pojo;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 12/4/2021 10:33 PM
 */
public class Student {
    private String name;
    private Integer age;
    private Boolean flag;

    public Student() {
    }

    public Student(String name, Integer age, Boolean flag) {
        this.name = name;
        this.age = age;
        this.flag = flag;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public Boolean getFlag() {
        return flag;
    }

    public void setFlag(Boolean flag) {
        this.flag = flag;
    }
}
