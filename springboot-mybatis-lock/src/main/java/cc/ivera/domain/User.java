package cc.ivera.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;


import java.beans.Transient;
import java.io.Serializable;


public class User implements Serializable {

    private Integer id;
    private String username;
    private String password;
    @JsonIgnore
    private String ipAddress;

    private String fieldNotInDb;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress=ipAddress;
    }

    public String getIpAddress() {
        return this.ipAddress;
    }


    @Transient
    public String getFieldNotInDb(){
        return this.fieldNotInDb;
    }


}
