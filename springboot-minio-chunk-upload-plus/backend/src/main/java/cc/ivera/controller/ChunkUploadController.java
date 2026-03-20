package cc.ivera.controller;

import cc.ivera.api.ApiResponse;
import cc.ivera.dto.InitUploadRequest;
import cc.ivera.dto.InitUploadResponse;
import cc.ivera.entity.FileResource;
import cc.ivera.service.ChunkService;
import cc.ivera.service.FileResourceService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@RestController
@CrossOrigin
@RequestMapping("/api/chunk")
@RequiredArgsConstructor
public class ChunkUploadController {

    private final ChunkService chunkService;
    private final FileResourceService fileResourceService;

    @PostMapping("/md5")
    public ApiResponse<String> calcMd5(@RequestParam("file") MultipartFile file) {
        try {
            // 读取文件流并计算真实 MD5
            String md5 = DigestUtils.md5Hex(file.getInputStream());
            return ApiResponse.ok(md5);
        } catch (Exception e) {
            return ApiResponse.fail("计算 MD5 失败: " + e.getMessage());
        }
    }


    /**
     * 初始化上传：返回 uploadId、分片数量等
     */
    @PostMapping("/init")
    public ApiResponse<InitUploadResponse> init(@RequestBody @Valid InitUploadRequest request) throws Exception {
        InitUploadResponse resp = chunkService.initUpload(request);
        return ApiResponse.ok(resp);
    }

    /**
     * 上传单个分片
     */
    @PostMapping("/upload")
    public ApiResponse<Void> uploadChunk(@RequestParam("uploadId") String uploadId, @RequestParam("chunkIndex") int chunkIndex, @RequestParam("file") MultipartFile file) throws Exception {
        chunkService.uploadChunk(uploadId, chunkIndex, file);
        return ApiResponse.ok(null);
    }

    /**
     * 合并分片
     */
    @PostMapping("/merge")
    public ApiResponse<String> merge(
            @RequestParam("uploadId") String uploadId,
            @RequestParam("totalChunks") int totalChunks,
            @RequestParam("fileName") String encodedName) throws Exception {
        // ⚠ 先 URL decode，再 Base64 decode，得到原始 UTF-8 中文文件名
        String fileName = URLDecoder.decode(
                new String(Base64.getDecoder().decode(encodedName), StandardCharsets.UTF_8),
                StandardCharsets.UTF_8.name()
        );

        String url = chunkService.mergeChunks(uploadId, totalChunks, fileName);
        return ApiResponse.ok(url);
    }

    /**
     * 根据文件记录 ID 获取访问 URL
     */
    @GetMapping("/file/{id}/url")
    public ApiResponse<String> fileUrl(@PathVariable("id") Long id) throws Exception {
        FileResource fr = fileResourceService.getById(id);
        if (fr == null) {
            return ApiResponse.fail("file not found");
        }
        String url = chunkService.createPresignedUrl(fr.getObjectName());
        return ApiResponse.ok(url);
    }
}
