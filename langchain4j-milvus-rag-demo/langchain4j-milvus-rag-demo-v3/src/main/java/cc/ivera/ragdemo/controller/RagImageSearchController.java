package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.model.query.RagSearchRequest;
import cc.ivera.ragdemo.model.query.RagSearchResponse;
import cc.ivera.ragdemo.service.query.ImageSearchResultGrouper;
import cc.ivera.ragdemo.service.query.RagImageSearchResponse;
import cc.ivera.ragdemo.service.query.RagQueryService;
import cc.ivera.ragdemo.util.TraceUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/api/rag/image-search")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG 图片检索", description = "按上传图片、图片 URL 或图片资产 ID 查询相似图片和相关知识。")
public class RagImageSearchController {

    private final RagQueryService ragQueryService;
    private final ImageSearchResultGrouper resultGrouper;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传图片检索", description = "上传本地图片，返回相似图片和相关知识分组。")
    public RagApiResponse<RagImageSearchResponse> searchMultipart(@RequestPart("file") MultipartFile file,
                                                                  @RequestParam("knowledgeBaseIds") List<Long> knowledgeBaseIds,
                                                                  @RequestParam(value = "question", required = false) String question,
                                                                  @RequestParam(value = "retrievalMode", defaultValue = "vector") String retrievalMode,
                                                                  @RequestParam(value = "topK", required = false) Integer topK,
                                                                  @RequestParam(value = "minScore", required = false) Double minScore,
                                                                  @RequestParam(value = "includeReviewPending", defaultValue = "false") Boolean includeReviewPending,
                                                                  @RequestParam(value = "contentTypes", required = false) List<String> contentTypes,
                                                                  @RequestParam(value = "permissionTags", required = false) List<String> permissionTags) {
        String traceId = TraceUtils.currentTraceId();
        RagSearchRequest searchRequest = new RagSearchRequest(
                null,
                knowledgeBaseIds,
                blankToNull(question),
                null,
                null,
                toDataUrl(file),
                List.of("image"),
                blankToDefault(retrievalMode, "vector"),
                topK,
                minScore,
                0.0D,
                1.0D,
                0.0D,
                includeReviewPending,
                normalizeContentTypes(contentTypes),
                permissionTags == null ? List.of() : permissionTags
        );
        RagSearchResponse searchResponse = ragQueryService.search(searchRequest, traceId);
        return RagApiResponse.ok(traceId, resultGrouper.group(searchResponse.queryLogId(), searchResponse.items()));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "按图片引用检索", description = "使用 imageBase64、imageUrl 或 imageAssetId 返回相似图片和相关知识分组。")
    public RagApiResponse<RagImageSearchResponse> searchJson(@Valid @RequestBody RagImageSearchRequest request) {
        String traceId = TraceUtils.currentTraceId();
        RagSearchRequest searchRequest = new RagSearchRequest(
                null,
                request.knowledgeBaseIds(),
                blankToNull(request.question()),
                request.imageUrl(),
                request.imageAssetId(),
                request.imageBase64(),
                List.of("image"),
                blankToDefault(request.retrievalMode(), "vector"),
                request.topK(),
                request.minScore(),
                0.0D,
                1.0D,
                0.0D,
                request.includeReviewPending(),
                normalizeContentTypes(request.contentTypes()),
                request.permissionTags() == null ? List.of() : request.permissionTags()
        );
        RagSearchResponse searchResponse = ragQueryService.search(searchRequest, traceId);
        return RagApiResponse.ok(traceId, resultGrouper.group(searchResponse.queryLogId(), searchResponse.items()));
    }

    private String toDataUrl(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file must not be empty");
        }
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType() : "application/octet-stream";
        if (!contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("file must be an image");
        }
        try {
            return "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(file.getBytes());
        } catch (Exception ex) {
            throw new IllegalArgumentException("failed to read image file", ex);
        }
    }

    private List<String> normalizeContentTypes(List<String> contentTypes) {
        if (contentTypes == null || contentTypes.isEmpty()) {
            return List.of("image", "chart", "table", "flowchart", "architecture");
        }
        return contentTypes;
    }

    private String blankToDefault(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value.trim() : defaultValue;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
