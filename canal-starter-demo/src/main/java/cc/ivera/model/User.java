package cc.ivera.model;

import jdk.jfr.DataAmount;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.Table;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Table(name = "t_user")
public class User {
    private Long id;
    private String name;
    private Integer age;
    // getter/setter
}
