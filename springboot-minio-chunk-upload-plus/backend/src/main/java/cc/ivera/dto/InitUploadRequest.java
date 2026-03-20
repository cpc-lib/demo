package cc.ivera.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InitUploadRequest {

    @NotBlank
    private String fileName;

    @Min(1)
    private long fileSize;

    private String fileMd5;

    @Min(1)
    private int chunkSize;
}
