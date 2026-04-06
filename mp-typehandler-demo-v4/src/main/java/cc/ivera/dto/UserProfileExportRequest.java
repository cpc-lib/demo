package cc.ivera.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileExportRequest {

    @NotNull(message = "pageNo不能为空")
    private Integer pageNo;

    @NotNull(message = "pageSize不能为空")
    private Integer pageSize;

    private String name;

    @NotEmpty(message = "fields不能为空")
    private List<String> fields;
}
