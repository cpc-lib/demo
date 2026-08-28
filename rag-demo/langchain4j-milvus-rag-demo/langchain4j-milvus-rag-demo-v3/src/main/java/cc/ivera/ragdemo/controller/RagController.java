package cc.ivera.ragdemo.controller;


import cc.ivera.ragdemo.annotation.RateLimit;
import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.model.query.ChatRequest;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.model.query.RagApiResponse;
import cc.ivera.ragdemo.service.MilvusCollectionQueryService;
import cc.ivera.ragdemo.service.RagChatService;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
@Tag(name = "Runtime Chat and Milvus Ops", description = "Chat, static assets, vector store configuration, and Milvus collection operations.")
public class RagController {

    private final RagChatService ragChatService;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final MilvusCollectionQueryService milvusCollectionQueryService;
    private final RagProperties ragProperties;

    @GetMapping("/knowledge/assets/{documentId}/{fileName}")
    @Operation(summary = "Read knowledge asset", description = "Read local assets extracted by multimodal ingestion.")
    public ResponseEntity<Resource> knowledgeAsset(@PathVariable String documentId,
                                                   @PathVariable String fileName) {
        Path root = Path.of(ragProperties.getMultimodalIngest().getAssetDirectory()).toAbsolutePath().normalize();
        Path file = root.resolve(documentId).resolve(fileName).normalize();
        if (!file.startsWith(root) || !file.toFile().isFile()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(new FileSystemResource(file));
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream chat", description = "Return chat chunks over SSE.")
    @RateLimit(max = 30, windowSeconds = 60, key = RateLimit.KeyType.IP_URI)
    public SseEmitter chatStream(@RequestParam("question") String question,
                                 @RequestParam(value = "conversationId", required = false) String conversationId) {
        return ragChatService.streamAnswer(conversationId, question);
    }

    @PostMapping("/chat")
    @Operation(summary = "Chat", description = "Return a plain text chat answer.")
    @RateLimit(max = 60, windowSeconds = 60, key = RateLimit.KeyType.IP_URI)
    public RagApiResponse<String> chat(@Valid @RequestBody ChatRequest request) {
        return RagApiResponse.ok(ragChatService.answer(request.getConversationId(), request.getQuestion()));
    }

    @PostMapping("/chat/detail")
    @Operation(summary = "Detailed chat", description = "Return answer, retrieval hits, tool traces, and sources.")
    @RateLimit(max = 60, windowSeconds = 60, key = RateLimit.KeyType.IP_URI)
    public RagApiResponse<ChatAnswer> chatDetail(@Valid @RequestBody ChatRequest request) {
        return RagApiResponse.ok(ragChatService.answerDetailed(request.getConversationId(), request.getQuestion()));
    }

    @GetMapping("/vector-stores/current")
    @Operation(summary = "Get active vector store", description = "Return the active Milvus alias and configuration.")
    public RagApiResponse<Map<String, Object>> currentVectorStore() {
        ActiveMilvusContext context = dynamicMilvusStoreManager.current();
        return RagApiResponse.ok(Map.of(
                "activeAlias", context.alias(),
                "config", context.config()
        ));
    }

    @GetMapping("/vector-stores")
    @Operation(summary = "List vector stores", description = "List available Milvus configurations with safe pagination and sorting.")
    public RagApiResponse<PageResponse<?>> listVectorStores(@RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                            @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                            @RequestParam(value = "limit", required = false) Integer limit,
                                                            @RequestParam(value = "sortBy", required = false) String sortBy,
                                                            @RequestParam(value = "sortDirection", required = false) String sortDirection) {
        return RagApiResponse.ok(PageResponse.slice(
                dynamicMilvusStoreManager.listAll(),
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500)
        ));
    }

    @PostMapping("/vector-stores")
    @Operation(summary = "Save vector store", description = "Create or update a Milvus vector store configuration.")
    public RagApiResponse<Map<String, Object>> saveVectorStore(@Valid @RequestBody MilvusConfigRequest request) {
        dynamicMilvusStoreManager.saveOrUpdate(request.toStoreConfig());
        return RagApiResponse.ok(Map.of("alias", request.alias()));
    }

    @PostMapping("/vector-stores/switch")
    @Operation(summary = "Switch active vector store", description = "Switch the active Milvus configuration by alias.")
    public RagApiResponse<Map<String, Object>> switchVectorStore(@RequestParam("alias") String alias) {
        dynamicMilvusStoreManager.switchTo(alias);
        return RagApiResponse.ok(Map.of("activeAlias", alias));
    }

    @GetMapping("/milvus/collections")
    @Operation(summary = "List Milvus collections", description = "List Milvus collections with safe pagination and sorting.")
    public RagApiResponse<PageResponse<?>> listCollections(@RequestParam(value = "databaseName", required = false) String databaseName,
                                                           @RequestParam(value = "pageNo", required = false) Integer pageNo,
                                                           @RequestParam(value = "pageSize", required = false) Integer pageSize,
                                                           @RequestParam(value = "limit", required = false) Integer limit,
                                                           @RequestParam(value = "sortBy", required = false) String sortBy,
                                                           @RequestParam(value = "sortDirection", required = false) String sortDirection) throws InterruptedException {
        return RagApiResponse.ok(PageResponse.slice(
                milvusCollectionQueryService.listCollections(databaseName),
                PageQuery.of(pageNo, pageSize, limit, sortBy, sortDirection, 500)
        ));
    }

    @GetMapping("/milvus/collections/{collectionName}")
    @Operation(summary = "Describe Milvus collection", description = "Return schema and status for one Milvus collection.")
    public RagApiResponse<?> describeCollection(@PathVariable String collectionName,
                                                @RequestParam(value = "databaseName", required = false) String databaseName) throws InterruptedException {
        return RagApiResponse.ok(milvusCollectionQueryService.describeCollection(databaseName, collectionName));
    }

    @PostMapping("/milvus/collections/query")
    @Operation(summary = "Query Milvus collection", description = "Query a Milvus collection by filter and output fields.")
    public RagApiResponse<?> queryCollection(@Valid @RequestBody MilvusCollectionQueryRequest request) throws InterruptedException {
        return RagApiResponse.ok(milvusCollectionQueryService.query(request));
    }

    @PostMapping("/milvus/collections")
    @Operation(summary = "Create Milvus collection", description = "Create a Milvus collection from the requested schema.")
    public RagApiResponse<?> createCollection(@Valid @RequestBody MilvusCreateCollectionRequest request) throws InterruptedException {
        return RagApiResponse.ok(milvusCollectionQueryService.createCollection(request));
    }
}
