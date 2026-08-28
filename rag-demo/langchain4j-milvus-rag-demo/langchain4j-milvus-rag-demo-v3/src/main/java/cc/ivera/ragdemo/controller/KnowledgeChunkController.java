package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.model.knowledge.ChunkStatus;
import cc.ivera.ragdemo.model.knowledge.KnowledgeChunkRecord;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.KnowledgeChunkManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag/chunks")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "RAG Chunk 管理", description = "Chunk 当前版本、历史版本、回滚、禁用、删除和 Redis 索引重建接口")
public class KnowledgeChunkController {

    private final KnowledgeChunkManagementService chunkManagementService;

    @GetMapping
    @Operation(summary = "分页查询 Chunk", description = "按文档、内容类型、状态和是否包含历史版本过滤 Chunk，支持分页和数据库侧排序。")
    public RagApiResponse<PageResponse<KnowledgeChunkRecord>> listChunks(@RequestParam(value = "documentId", required = false) String documentId,
                                                                         @RequestParam(value = "contentType", required = false) String contentType,
                                                                         @RequestParam(value = "status", required = false) ChunkStatus status,
                                                                         @RequestParam(value = "includeHistory", defaultValue = "false") boolean includeHistory,
                                                                         @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                         @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                         @RequestParam(value = "limit", required = false) Integer limit,
                                                                         @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                         @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(chunkManagementService.pageList(
                documentId,
                contentType,
                status,
                includeHistory,
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500)
        ));
    }

    @GetMapping("/{chunkId}")
    @Operation(summary = "获取 Chunk 当前或指定版本", description = "未传 version 时返回当前活跃版本，传 version 时返回指定历史版本。")
    public RagApiResponse<KnowledgeChunkRecord> getChunk(@PathVariable String chunkId,
                                                         @RequestParam(value = "version", required = false) Integer version) {
        KnowledgeChunkRecord record = version == null
                ? chunkManagementService.activeRecord(chunkId).orElseThrow(() -> new IllegalArgumentException("Active chunk not found: " + chunkId))
                : chunkManagementService.getVersion(chunkId, version).orElseThrow(() -> new IllegalArgumentException("Chunk version not found: " + chunkId + " v" + version));
        return RagApiResponse.ok(record);
    }

    @GetMapping("/{chunkId}/versions")
    @Operation(summary = "分页查询 Chunk 版本", description = "查询指定 Chunk 的版本历史，支持分页和数据库侧排序。")
    public RagApiResponse<PageResponse<KnowledgeChunkRecord>> listVersions(@PathVariable String chunkId,
                                                                           @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                                           @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                                           @RequestParam(value = "limit", required = false) Integer limit,
                                                                           @RequestParam(value = "sortBy", required = false) String sortBy,
                                                                           @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(chunkManagementService.pageVersions(
                chunkId,
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500)
        ));
    }

    @PostMapping
    @Operation(summary = "创建手工 Chunk", description = "创建新的手工 Chunk，生成版本并写入向量。")
    public RagApiResponse<KnowledgeChunkRecord> createChunk(@Valid @RequestBody KnowledgeChunkCreateRequest request) {
        KnowledgeChunkRecord chunk = chunkManagementService.createChunk(request);
        return RagApiResponse.ok(chunk);
    }

    @PostMapping("/rebuild-redis-index")
    @Operation(summary = "从 MySQL 重建 Chunk Redis 索引", description = "以 MySQL Chunk 版本为权威源重建 Redis 当前版本注册表。")
    public RagApiResponse<?> rebuildRedisIndex(@RequestParam(value = "clearExisting", defaultValue = "true") boolean clearExisting) {
        return RagApiResponse.ok(chunkManagementService.rebuildRedisRegistryFromMysql(clearExisting));
    }

    @PutMapping("/{chunkId}")
    @Operation(summary = "更新 Chunk 并创建新版本", description = "更新 Chunk 内容或元数据，创建新的活跃版本并替换旧向量。")
    public RagApiResponse<KnowledgeChunkRecord> updateChunk(@PathVariable String chunkId,
                                                            @Valid @RequestBody KnowledgeChunkUpdateRequest request) {
        KnowledgeChunkRecord chunk = chunkManagementService.updateChunk(chunkId, request);
        return RagApiResponse.ok(chunk);
    }

    @PostMapping("/{chunkId}/rollback")
    @Operation(summary = "回滚 Chunk 版本", description = "以指定历史版本为基础创建新的活跃版本。")
    public RagApiResponse<KnowledgeChunkRecord> rollback(@PathVariable String chunkId,
                                                         @RequestParam("version") int version) {
        KnowledgeChunkRecord chunk = chunkManagementService.rollback(chunkId, version);
        return RagApiResponse.ok(chunk);
    }

    @PostMapping("/{chunkId}/disable")
    @Operation(summary = "禁用 Chunk", description = "创建禁用版本并移除当前活跃向量。")
    public RagApiResponse<KnowledgeChunkRecord> disableChunk(@PathVariable String chunkId) {
        KnowledgeChunkRecord chunk = chunkManagementService.disableChunk(chunkId);
        return RagApiResponse.ok(chunk);
    }

    @DeleteMapping("/{chunkId}")
    @Operation(summary = "删除 Chunk", description = "创建删除版本并移除当前活跃向量。")
    public RagApiResponse<KnowledgeChunkRecord> deleteChunk(@PathVariable String chunkId) {
        KnowledgeChunkRecord chunk = chunkManagementService.deleteChunk(chunkId);
        return RagApiResponse.ok(chunk);
    }
}
