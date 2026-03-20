package cc.ivera.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreeBean implements Serializable {
    private java.lang.String id;
    private java.lang.String pId;
    private java.lang.String name;
    private boolean parent;
    private java.lang.Object obj;
    private boolean selected;
}
