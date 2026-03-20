package cc.ivera.model.xml;

import lombok.Data;

import java.util.List;

@Data
public class Hobbies {
    private String userId;
    private List<Hobby> hobbyList;
}
