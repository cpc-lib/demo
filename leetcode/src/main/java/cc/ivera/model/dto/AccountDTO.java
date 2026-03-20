package cc.ivera.model.dto;

import cn.afterturn.easypoi.excel.annotation.Excel;

import java.util.Objects;

public class AccountDTO {

    @Excel(name = "学号", orderNum = "0")
    private Integer id;

    @Excel(name = "姓名", orderNum = "1")
    private String userName;

    public AccountDTO() {
    }


    public AccountDTO(Integer id, String userName) {
        this.id = id;
        this.userName = userName;
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


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountDTO that = (AccountDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(userName, that.userName);
    }


    @Override
    public int hashCode() {
        return Objects.hash(id, userName);
    }


    @Override
    public String toString() {
        return "AccountDTO{" + "id=" + id + ", userName='" + userName + '\'' + '}';
    }
}
