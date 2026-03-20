package cc.ivera.dto;

import lombok.Data;

@Data
public class InitUploadResponse {

    private boolean exist;

    private String fileUrl;

    private String uploadId;

    private String bucket;

    private String objectName;

    private int totalChunks;
}
