package cc.ivera.service;

import cc.ivera.dto.InitUploadRequest;
import cc.ivera.dto.InitUploadResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ChunkService {

    InitUploadResponse initUpload(InitUploadRequest request) throws Exception;

    void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunk) throws Exception;

    String mergeChunks(String uploadId, int totalChunks, String originalFileName) throws Exception;

    String createPresignedUrl(String objectName) throws Exception;
}
