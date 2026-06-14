
package cc.ivera.dto;
import lombok.Data;

@Data
public class PhotoTypeSaveDTO {
    private String id;
    private String parentId;
    private String name;
    private String nameEn;
    private Integer sort;
    private String nameRule;
    private String nameRuleEn;
}
