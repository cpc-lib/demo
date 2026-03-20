package cc.ivera.model.dto;

import cn.afterturn.easypoi.excel.annotation.Excel;

public class UserDTO {
    @Excel(name = "学号", orderNum = "0")
    private Integer id;
    @Excel(name = "姓名", orderNum = "1")
    private String userName;
    @Excel(name = "年龄", orderNum = "2")
    private String userAge;

    public UserDTO() {
    }

    public UserDTO(Integer id, String userName, String userAge) {
        this.id = id;
        this.userName = userName;
        this.userAge = userAge;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", userName='" + userName + '\'' +
                ", userAge='" + userAge + '\'' +
                '}';
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserAge() {
        return userAge;
    }

    public void setUserAge(String userAge) {
        this.userAge = userAge;
    }
}