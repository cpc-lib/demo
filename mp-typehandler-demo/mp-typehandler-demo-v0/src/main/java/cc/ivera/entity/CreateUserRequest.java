package cc.ivera.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {
    private String name;
    private List<String> hobbies;
    private List<TagItem> tags;
}