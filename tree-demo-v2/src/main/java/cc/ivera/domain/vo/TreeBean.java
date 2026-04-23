package cc.ivera.domain.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreeBean implements Serializable {
    private String id;
    private String pId;
    private String name;
    private boolean parent;
    private Object obj;
    private boolean selected;
}
