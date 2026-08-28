package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.domain.rag.RagDocument;
import cc.ivera.ragdemo.domain.rag.RagDocumentVersion;
import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.domain.rag.RagIngestionTaskShard;
import cc.ivera.ragdemo.mapper.RagDocumentMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.model.dto.*;
import cc.ivera.ragdemo.model.knowledge.*;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.*;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/rag")
@Slf4j
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG 文档与摄入任务", description = "文档上传、版本管理、摄入任务治理和文件下载接口")
public class RagDocumentController {

    private final RagDocumentIngestionService ingestionService;
    private final RagDocumentVersionService documentVersionService;
    private final RagIngestionTaskGovernanceService taskGovernanceService;
    private final IngestionTaskProgressService taskProgressService;
    private final IngestionTaskEventPublisher taskEventPublisher;
    private final IngestionShardRetryService shardRetryService;
    private final RagKnowledgeBaseService knowledgeBaseService;
    private final RagDocumentMapper documentMapper;
    private final RagIngestionTaskMapper taskMapper;
    private final EntityDtoConverter converter;

    @PostMapping(value = "/documents/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档到默认知识库", description = "保存原始文件，创建文档、版本和异步摄入任务。")
    public RagApiResponse<RagIngestionSubmitResponse> ingestToDefault(@RequestPart("file") MultipartFile file) {
        RagIngestionSubmitResponse response = ingestionService.submitToDefaultKnowledgeBase(file);
        return RagApiResponse.ok(response);
    }

    @PostMapping(value = "/documents/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit text to default knowledge base", description = "Save text as a document and create an async ingestion task.")
    public RagApiResponse<RagIngestionSubmitResponse> ingestTextToDefault(@Valid @RequestBody RagTextDocumentIngestRequest request) {
        RagIngestionSubmitResponse response = ingestionService.submitTextToDefaultKnowledgeBase(request.text(), request.fileName());
        return RagApiResponse.ok(response);
    }

    @PostMapping(value = "/knowledge-bases/{knowledgeBaseId}/documents/ingest", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文档到指定知识库", description = "将文档绑定到指定知识库并提交异步摄入任务。")
    public RagApiResponse<RagIngestionSubmitResponse> ingest(@PathVariable Long knowledgeBaseId,
                                                             @RequestPart("file") MultipartFile file) {
        RagIngestionSubmitResponse response = ingestionService.submit(knowledgeBaseId, file);
        return RagApiResponse.ok(response);
    }

    @PostMapping(value = "/knowledge-bases/{knowledgeBaseId}/documents/ingest", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Submit text to knowledge base", description = "Save text as a document in the selected knowledge base and create an async ingestion task.")
    public RagApiResponse<RagIngestionSubmitResponse> ingestTextToKnowledgeBase(@PathVariable Long knowledgeBaseId,
                                                                                @Valid @RequestBody RagTextDocumentIngestRequest request) {
        RagIngestionSubmitResponse response = ingestionService.submitText(knowledgeBaseId, request.text(), request.fileName());
        return RagApiResponse.ok(response);
    }

    @GetMapping("/knowledge-bases/{knowledgeBaseId}/documents")
    @Operation(summary = "分页查询知识库文档", description = "按知识库和状态过滤文档，支持分页与数据库侧白名单排序。")
    public RagApiResponse<PageResponse<RagDocumentDto>> listDocuments(@PathVariable Long knowledgeBaseId,
                                                                     @RequestParam(value = "status", required = false) Integer status,
                                                                     @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                     @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                     @RequestParam(value = "limit", required = false) Integer limit,
                                                                     @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                     @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(pageDocuments(
                knowledgeBaseId,
                status,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        ));
    }

    @GetMapping("/documents/{documentId}")
    @Operation(summary = "获取文档详情", description = "按文档 ID 获取文档元数据。")
    public RagApiResponse<RagDocumentDto> getDocument(@PathVariable Long documentId) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.getRequiredDocument(documentId)));
    }

    @GetMapping("/documents/{documentId}/versions")
    @Operation(summary = "分页查询文档版本", description = "查询指定文档的版本列表，支持分页和数据库侧排序。")
    public RagApiResponse<PageResponse<RagDocumentVersionDto>> listDocumentVersions(@PathVariable Long documentId,
                                                                                   @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                                   @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                                   @RequestParam(value = "limit", required = false) Integer limit,
                                                                                   @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                                   @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<RagDocumentVersion> page = documentVersionService.pageVersions(
                documentId,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        );
        List<RagDocumentVersionDto> dtoList = converter.toDocumentVersionDtoList(page.records());
        PageResponse<RagDocumentVersionDto> dtoPage = new PageResponse<>(
                page.pageNo(), page.pageSize(), page.total(), page.pages(),
                page.maxPageSize(), page.sortBy(), page.sortDirection(), dtoList
        );
        return RagApiResponse.ok(dtoPage);
    }

    @GetMapping("/documents/{documentId}/versions/{versionNo}")
    @Operation(summary = "获取文档指定版本", description = "按文档 ID 和版本号获取文档版本元数据。")
    public RagApiResponse<RagDocumentVersionDto> getDocumentVersion(@PathVariable Long documentId,
                                                                     @PathVariable Integer versionNo) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.getRequiredVersion(documentId, versionNo)));
    }

    @GetMapping("/documents/{documentId}/versions/diff")
    @Operation(summary = "Compare document versions", description = "Return a line-level text diff between two document versions.")
    public RagApiResponse<RagDocumentVersionDiffResponse> diffDocumentVersions(@PathVariable Long documentId,
                                                                               @RequestParam("leftVersionNo") Integer leftVersionNo,
                                                                               @RequestParam("rightVersionNo") Integer rightVersionNo) {
        return RagApiResponse.ok(documentVersionService.diff(documentId, leftVersionNo, rightVersionNo));
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/note")
    @Operation(summary = "Update document version note", description = "Update the operator note for a document version.")
    public RagApiResponse<RagDocumentVersionDto> updateDocumentVersionNote(@PathVariable Long documentId,
                                                                            @PathVariable Integer versionNo,
                                                                            @RequestBody(required = false) RagDocumentVersionNoteRequest request) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.updateNote(documentId, versionNo, request)));
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/submit-review")
    @Operation(summary = "Submit document version for review", description = "Move a DRAFT or REJECTED document version to PENDING_REVIEW.")
    public RagApiResponse<RagDocumentVersionDto> submitDocumentVersionReview(@PathVariable Long documentId,
                                                                              @PathVariable Integer versionNo,
                                                                              @RequestBody(required = false) RagDocumentVersionReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.submitReview(documentId, versionNo, request)));
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/approve")
    @Operation(summary = "Approve document version", description = "Approve a PENDING_REVIEW document version.")
    public RagApiResponse<RagDocumentVersionDto> approveDocumentVersion(@PathVariable Long documentId,
                                                                         @PathVariable Integer versionNo,
                                                                         @RequestBody(required = false) RagDocumentVersionReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.approve(documentId, versionNo, request)));
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/reject")
    @Operation(summary = "Reject document version", description = "Reject a PENDING_REVIEW document version.")
    public RagApiResponse<RagDocumentVersionDto> rejectDocumentVersion(@PathVariable Long documentId,
                                                                        @PathVariable Integer versionNo,
                                                                        @RequestBody(required = false) RagDocumentVersionReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.reject(documentId, versionNo, request)));
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/publish")
    @Operation(summary = "Publish document version", description = "Publish the current APPROVED document version without changing retrieval vectors.")
    public RagApiResponse<RagDocumentVersionDto> publishDocumentVersion(@PathVariable Long documentId,
                                                                         @PathVariable Integer versionNo,
                                                                         @RequestBody(required = false) RagDocumentVersionReviewRequest request) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.publish(documentId, versionNo, request)));
    }

    @PostMapping(value = "/documents/{documentId}/replace", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "替换文档并创建新版本", description = "上传替换文件，生成新文档版本并提交摄入任务。")
    public RagApiResponse<RagIngestionSubmitResponse> replaceDocument(@PathVariable Long documentId,
                                                                      @RequestPart("file") MultipartFile file) {
        RagIngestionSubmitResponse response = documentVersionService.replace(documentId, file);
        return RagApiResponse.ok(response);
    }

    @PostMapping("/documents/{documentId}/reparse")
    @Operation(summary = "重新解析文档", description = "基于当前文档版本重新提交解析和向量化任务。")
    public RagApiResponse<RagIngestionSubmitResponse> reparseDocument(@PathVariable Long documentId) {
        RagIngestionSubmitResponse response = documentVersionService.reparse(documentId);
        return RagApiResponse.ok(response);
    }

    @PostMapping("/documents/{documentId}/versions/{versionNo}/rollback")
    @Operation(summary = "回滚文档版本", description = "将文档回滚到指定版本，并提交后续摄入处理。")
    public RagApiResponse<RagIngestionSubmitResponse> rollbackDocument(@PathVariable Long documentId,
                                                                       @PathVariable Integer versionNo) {
        RagIngestionSubmitResponse response = documentVersionService.rollback(documentId, versionNo);
        return RagApiResponse.ok(response);
    }

    @PostMapping("/documents/{documentId}/disable")
    @Operation(summary = "禁用文档", description = "将文档标记为禁用。")
    public RagApiResponse<RagDocumentDto> disableDocument(@PathVariable Long documentId) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.disable(documentId)));
    }

    @PostMapping("/documents/{documentId}/enable")
    @Operation(summary = "启用文档", description = "将禁用文档恢复为启用。")
    public RagApiResponse<RagDocumentDto> enableDocument(@PathVariable Long documentId) {
        return RagApiResponse.ok(converter.toDto(documentVersionService.enable(documentId)));
    }

    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "删除文档", description = "软删除文档记录。")
    public RagApiResponse<RagDocumentDeleteResponse> deleteDocument(@PathVariable Long documentId) {
        return RagApiResponse.ok(documentVersionService.delete(documentId));
    }

    @PostMapping("/documents/batch")
    @Operation(summary = "Batch operate RAG documents", description = "Batch DELETE, DISABLE, ENABLE or REPARSE documents.")
    public RagApiResponse<RagDocumentBatchResponse> batchDocuments(@RequestBody RagDocumentBatchRequest request) {
        return RagApiResponse.ok(documentVersionService.batch(request));
    }

    @GetMapping("/documents/{documentId}/download")
    @Operation(summary = "下载文档文件", description = "下载当前或指定版本的文档原始文件，响应保持二进制文件格式。")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long documentId,
                                                   @RequestParam(value = "versionNo", required = false) Integer versionNo) {
        RagDocumentVersionDownload download = documentVersionService.download(documentId, versionNo);
        RagDocumentVersion version = download.version();
        String fileName = version.getOriginalFilename() == null ? version.getDocumentName() : version.getOriginalFilename();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + headerSafeFileName(fileName) + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(download.bytes());
    }

    @GetMapping("/ingestion-tasks/{taskId}")
    @Operation(summary = "获取摄入任务详情", description = "按任务 ID 查询摄入任务状态和错误信息。")
    public RagApiResponse<RagIngestionTaskDto> getTask(@PathVariable Long taskId) {
        RagIngestionTask task = taskGovernanceService.getRequired(taskId);
        return RagApiResponse.ok(converter.toDto(task));
    }

    @GetMapping("/ingestion-tasks/{taskId}/progress")
    @Operation(summary = "获取摄入任务进度快照", description = "返回任务主状态、阶段时间线和分片统计。")
    public RagApiResponse<IngestionTaskProgressSnapshot> getTaskProgress(@PathVariable Long taskId) {
        return RagApiResponse.ok(taskProgressService.snapshot(taskId));
    }

    @GetMapping(value = "/ingestion-tasks/{taskId}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "订阅摄入任务事件流", description = "通过 SSE 推送任务、阶段和分片进度事件，支持 Last-Event-ID 回放。")
    public SseEmitter subscribeTaskEvents(@PathVariable Long taskId,
                                          @RequestHeader(value = "Last-Event-ID", required = false) String lastEventId) throws IOException {
        RagIngestionTask task = taskGovernanceService.getRequired(taskId);
        SseEmitter emitter = taskEventPublisher.subscribe(task.getTenantId(), taskId, lastEventId);
        Long databaseEventId = parseLongOrNull(lastEventId);
        if (databaseEventId != null) {
            PageResponse<IngestionTaskEventView> replay = taskProgressService.pageEvents(
                    taskId,
                    databaseEventId,
                    PageQuery.of(1, 500, 500, "id", "ASC", 500)
            );
            for (IngestionTaskEventView event : replay.records()) {
                emitter.send(SseEmitter.event()
                        .id(event.id() == null ? "" : String.valueOf(event.id()))
                        .name(event.eventType())
                        .data(event));
            }
        }
        return emitter;
    }

    @GetMapping("/ingestion-tasks/{taskId}/events/history")
    @Operation(summary = "分页查询摄入任务事件历史", description = "用于 SSE 断线回放和任务排障。")
    public RagApiResponse<PageResponse<IngestionTaskEventView>> listTaskEvents(@PathVariable Long taskId,
                                                                               @RequestParam(value = "afterEventId", required = false) Long afterEventId,
                                                                               @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                               @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                               @RequestParam(value = "limit", required = false) Integer limit,
                                                                               @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                               @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        taskGovernanceService.getRequired(taskId);
        return RagApiResponse.ok(taskProgressService.pageEvents(
                taskId,
                afterEventId,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        ));
    }

    @GetMapping("/ingestion-tasks/{taskId}/shards")
    @Operation(summary = "分页查询摄入任务分片", description = "按阶段和分片状态过滤 Chunk 或图片资产处理单元。")
    public RagApiResponse<PageResponse<RagIngestionTaskShardDto>> listTaskShards(@PathVariable Long taskId,
                                                                                  @RequestParam(value = "stageCode", required = false) String stageCode,
                                                                                  @RequestParam(value = "status", required = false) String status,
                                                                                  @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                                  @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                                  @RequestParam(value = "limit", required = false) Integer limit,
                                                                                  @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                                  @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        taskGovernanceService.getRequired(taskId);
        PageResponse<RagIngestionTaskShard> page = taskProgressService.pageShards(
                taskId,
                stageCode,
                status,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        );
        List<RagIngestionTaskShardDto> dtoList = converter.toIngestionTaskShardDtoList(page.records());
        PageResponse<RagIngestionTaskShardDto> dtoPage = new PageResponse<>(
                page.pageNo(), page.pageSize(), page.total(), page.pages(),
                page.maxPageSize(), page.sortBy(), page.sortDirection(), dtoList
        );
        return RagApiResponse.ok(dtoPage);
    }

    @PostMapping("/ingestion-tasks/{taskId}/shards/retry")
    @Operation(summary = "重试摄入任务失败分片", description = "重置可重试分片并重新投递父任务消息。")
    public RagApiResponse<IngestionShardRetryResponse> retryTaskShards(@PathVariable Long taskId,
                                                                       @RequestBody(required = false) IngestionShardRetryRequest request) {
        return RagApiResponse.ok(shardRetryService.retryFailedShards(taskId, request));
    }

    @GetMapping("/ingestion-tasks")
    @Operation(summary = "分页查询摄入任务", description = "按租户、知识库、文档、状态和任务类型过滤任务，支持分页与数据库侧排序。")
    public RagApiResponse<PageResponse<RagIngestionTaskDto>> listTasks(@RequestParam(value = "tenantId", required = false) Long tenantId,
                                                                      @RequestParam(value = "knowledgeBaseId", required = false) Long knowledgeBaseId,
                                                                      @RequestParam(value = "documentId", required = false) Long documentId,
                                                                      @RequestParam(value = "status", required = false) Integer status,
                                                                      @RequestParam(value = "taskType", required = false) String taskType,
                                                                      @RequestParam(value = "limit", required = false) Integer limit,
                                                                      @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                      @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                      @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                      @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        PageResponse<RagIngestionTask> page = taskGovernanceService.pageTasks(
                tenantId,
                knowledgeBaseId,
                documentId,
                status,
                taskType,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        );
        List<RagIngestionTaskDto> dtoList = converter.toIngestionTaskDtoList(page.records());
        PageResponse<RagIngestionTaskDto> dtoPage = new PageResponse<>(
                page.pageNo(), page.pageSize(), page.total(), page.pages(),
                page.maxPageSize(), page.sortBy(), page.sortDirection(), dtoList
        );
        return RagApiResponse.ok(dtoPage);
    }

    @PostMapping("/ingestion-tasks/{taskId}/cancel")
    @Operation(summary = "取消摄入任务", description = "取消允许取消的摄入任务。")
    public RagApiResponse<RagIngestionTaskDto> cancelTask(@PathVariable Long taskId) {
        RagIngestionTask task = taskGovernanceService.cancel(taskId);
        return RagApiResponse.ok(converter.toDto(task));
    }

    @PostMapping("/ingestion-tasks/{taskId}/retry")
    @Operation(summary = "重试摄入任务", description = "将允许重试的失败任务重新发布到任务队列。")
    public RagApiResponse<RagIngestionTaskRetryResponse> retryTask(@PathVariable Long taskId) {
        RagIngestionTaskRetryResponse response = taskGovernanceService.retry(taskId);
        return RagApiResponse.ok(response);
    }

    @GetMapping("/documents/{documentId}/ingestion-tasks")
    @Operation(summary = "分页查询文档摄入任务", description = "查询指定文档关联的摄入任务，支持分页与数据库侧排序。")
    public RagApiResponse<PageResponse<RagIngestionTaskDto>> listDocumentTasks(@PathVariable Long documentId,
                                                                              @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                              @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                              @RequestParam(value = "limit", required = false) Integer limit,
                                                                              @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                              @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(pageDocumentTasks(
                documentId,
                pageQuery(pageNo, pageSize, limit, sortBy, sortDirection)
        ));
    }

    private PageQuery pageQuery(Integer pageNo, Integer pageSize, Integer limit, String sortBy, String sortDirection) {
        return PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500);
    }

    private PageResponse<RagDocumentDto> pageDocuments(Long knowledgeBaseId, Integer status, PageQuery pageQuery) {
        knowledgeBaseService.getRequired(knowledgeBaseId);
        PageQuery query = pageQuery.withDefaultSort("createdAt", "DESC");
        long total = documentMapper.selectCount(documentListQuery(knowledgeBaseId, status));
        LambdaQueryWrapper<RagDocument> rowsQuery = documentListQuery(knowledgeBaseId, status);
        applyDocumentOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        List<RagDocumentDto> dtoList = converter.toDocumentDtoList(documentMapper.selectList(rowsQuery));
        return PageResponse.of(query, total, dtoList);
    }

    private LambdaQueryWrapper<RagDocument> documentListQuery(Long knowledgeBaseId, Integer status) {
        return new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getKnowledgeBaseId, knowledgeBaseId)
                .eq(TenantContextHolder.currentTenantId().isPresent(),
                        RagDocument::getTenantId,
                        TenantContextHolder.currentTenantId().orElse(null))
                .eq(status != null, RagDocument::getDocumentStatus, status)
                .eq(RagDocument::getIsDeleted, 0);
    }

    private void applyDocumentOrder(LambdaQueryWrapper<RagDocument> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagDocument::getId);
            case "createdAt" -> wrapper.orderBy(true, asc, RagDocument::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagDocument::getUpdatedAt);
            case "documentName" -> wrapper.orderBy(true, asc, RagDocument::getDocumentName);
            case "fileSize" -> wrapper.orderBy(true, asc, RagDocument::getFileSize);
            case "chunkCount" -> wrapper.orderBy(true, asc, RagDocument::getChunkCount);
            case "documentStatus", "status" -> wrapper.orderBy(true, asc, RagDocument::getDocumentStatus);
            default -> wrapper.orderByDesc(RagDocument::getCreatedAt);
        }
    }

    private PageResponse<RagIngestionTaskDto> pageDocumentTasks(Long documentId, PageQuery pageQuery) {
        RagDocument document = documentVersionService.getRequiredDocument(documentId);
        PageQuery query = pageQuery.withDefaultSort("createdAt", "DESC");
        long total = taskMapper.selectCount(new LambdaQueryWrapper<RagIngestionTask>()
                .eq(RagIngestionTask::getDocumentId, documentId)
                .eq(TenantContextHolder.currentTenantId().isPresent(),
                        RagIngestionTask::getTenantId,
                        document.getTenantId()));
        LambdaQueryWrapper<RagIngestionTask> rowsQuery = new LambdaQueryWrapper<RagIngestionTask>()
                .eq(RagIngestionTask::getDocumentId, documentId)
                .eq(TenantContextHolder.currentTenantId().isPresent(),
                        RagIngestionTask::getTenantId,
                        document.getTenantId());
        applyTaskOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        List<RagIngestionTaskDto> dtoList = converter.toIngestionTaskDtoList(taskMapper.selectList(rowsQuery));
        return PageResponse.of(query, total, dtoList);
    }

    private void applyTaskOrder(LambdaQueryWrapper<RagIngestionTask> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagIngestionTask::getId);
            case "createdAt" -> wrapper.orderBy(true, asc, RagIngestionTask::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagIngestionTask::getUpdatedAt);
            case "progress" -> wrapper.orderBy(true, asc, RagIngestionTask::getProgress);
            case "taskStatus", "status" -> wrapper.orderBy(true, asc, RagIngestionTask::getTaskStatus);
            case "retryCount" -> wrapper.orderBy(true, asc, RagIngestionTask::getRetryCount);
            default -> wrapper.orderByDesc(RagIngestionTask::getCreatedAt);
        }
    }

    private String headerSafeFileName(String value) {
        String fileName = value == null ? "document" : value;
        return fileName.replaceAll("[\\\\/\\r\\n\"]", "_");
    }

    private Long parseLongOrNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            log.debug("Invalid long value: {}", value);
            return null;
        }
    }
}
