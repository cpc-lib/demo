package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.domain.rag.RagImageAsset;
import cc.ivera.ragdemo.model.dto.EntityDtoConverter;
import cc.ivera.ragdemo.model.dto.RagImageAssetDto;
import cc.ivera.ragdemo.model.knowledge.ImageAssetReprocessRequest;
import cc.ivera.ragdemo.model.knowledge.ImageAssetReviewRequest;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.RagImageAssetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag/image-assets")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG 多模态图片资产", description = "图片资产、OCR、视觉 JSON、置信度和向量映射查询接口")
public class RagImageAssetController {

    private final RagImageAssetService imageAssetService;
    private final EntityDtoConverter converter;

    @GetMapping
    @Operation(summary = "分页查询图片资产", description = "按租户、知识库、源文档、内容类型、视觉状态和最低置信度过滤图片资产，支持数据库侧分页排序。")
    public RagApiResponse<PageResponse<RagImageAssetDto>> list(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                            @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                            @RequestParam(value = "sourceDocumentId", required = false) String sourceDocumentId,
                                                            @RequestParam(value = "contentType", required = false) String contentType,
                                                            @RequestParam(value = "visualStatus", required = false) String visualStatus,
                                                            @RequestParam(value = "reviewStatus", required = false) String reviewStatus,
                                                            @RequestParam(value = "ocrStatus", required = false) String ocrStatus,
                                                            @RequestParam(value = "imageEmbeddingStatus", required = false) String imageEmbeddingStatus,
                                                            @RequestParam(value = "minConfidence", required = false) Double minConfidence,
                                                            @RequestParam(value = "limit", required = false) Integer limit,
                                                            @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                            @RequestParam(value = "sortBy", required = false) String sortBy,
                                                            @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<RagImageAsset> page = imageAssetService.pageAssets(
                tenantId,
                knowledgeBaseId,
                sourceDocumentId,
                contentType,
                visualStatus,
                reviewStatus,
                ocrStatus,
                imageEmbeddingStatus,
                minConfidence,
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 200)
        );
        return RagApiResponse.ok(new PageResponse<>(
                page.pageNo(),
                page.pageSize(),
                page.total(),
                page.pages(),
                page.maxPageSize(),
                page.sortBy(),
                page.sortDirection(),
                converter.toImageAssetDtoList(page.records())
        ));
    }

    @GetMapping("/review-pending")
    @Operation(summary = "List image assets pending review", description = "Return image assets that need manual review because of low confidence or schema issues.")
    public RagApiResponse<PageResponse<RagImageAssetDto>> reviewPending(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                     @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                     @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                     @RequestParam(value = "limit", required = false) Integer limit,
                                                                     @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                     @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<RagImageAsset> page = imageAssetService.pageReviewPending(
                tenantId,
                knowledgeBaseId,
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 200)
        );
        return RagApiResponse.ok(new PageResponse<>(
                page.pageNo(),
                page.pageSize(),
                page.total(),
                page.pages(),
                page.maxPageSize(),
                page.sortBy(),
                page.sortDirection(),
                converter.toImageAssetDtoList(page.records())
        ));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取图片资产详情", description = "按图片资产 ID 查询 OCR、视觉 JSON、置信度和向量映射。")
    public RagApiResponse<RagImageAssetDto> get(@PathVariable Long id) {
        return RagApiResponse.ok(converter.toDto(imageAssetService.getRequired(id)));
    }

    @PostMapping("/{id}/review/approve")
    @Operation(summary = "Approve an image asset", description = "Mark a reviewed image asset as approved for default retrieval.")
    public RagApiResponse<RagImageAssetDto> approve(@PathVariable Long id,
                                                 @RequestBody(required = false) ImageAssetReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(imageAssetService.approve(id, request)));
    }

    @PostMapping("/{id}/review/reject")
    @Operation(summary = "Reject an image asset", description = "Exclude an image asset from default retrieval until it is updated or reprocessed.")
    public RagApiResponse<RagImageAssetDto> reject(@PathVariable Long id,
                                                @RequestBody(required = false) ImageAssetReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(imageAssetService.reject(id, request)));
    }

    @PostMapping("/{id}/review/update")
    @Operation(summary = "Update reviewed image content", description = "Store manually corrected visual JSON and OCR text for an image asset.")
    public RagApiResponse<RagImageAssetDto> updateReview(@PathVariable Long id,
                                                      @RequestBody(required = false) ImageAssetReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(imageAssetService.updateReview(id, request)));
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "Reprocess an image asset", description = "Re-run OCR, visual analysis or image embedding for an existing image asset.")
    public RagApiResponse<RagImageAssetDto> reprocess(@PathVariable Long id,
                                                   @RequestBody(required = false) ImageAssetReprocessRequest request) {
        return RagApiResponse.ok(converter.toDto(imageAssetService.reprocess(id, request)));
    }
}
