package cc.ivera.dto;

import cc.ivera.entity.TagItem;
import lombok.Data;

import java.util.List;

@Data
public class UpdateUserRequest {
    private String name;
    private List<String> hobbies;
    private List<TagItem> tags;
}
