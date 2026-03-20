package cc.ivera.mongo.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(value = "person")
public class Person {

    private ObjectId _id;
    private String name;
    private Integer age;
    private String address;
    private Long userId;

}