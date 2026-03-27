
package cc.ivera.vo;

import lombok.Data;

import java.util.List;

@Data
public class PhotoTypeVO {
    private String id;
    private String name;
    private Integer sort;
    private String parentId;
    private Integer layer;
    private List<PhotoTypeVO> children;
}
