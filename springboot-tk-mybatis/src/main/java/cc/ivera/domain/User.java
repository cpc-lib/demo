package cc.ivera.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "tb_user")
public class User implements Serializable {

    @Id
    private Long id;
    @Column(name="username")
    private String username;
    @Column(name="password")
    private String password;
    @JsonIgnore
    //@Column(name="ip_address")
    @Column(name="ip_address")
    private String ipAddress;

    //设置该的值是否插入到数据库的对应字段,name不进行设置默认与实体类的字段相同。
    @Column(insertable=false)
    private String ip_address;


    //tkmybatis 根据表的schema进行查询。与mybatisplus不同。mybatisplus的查询与实体类的字段及顺序有关

}
