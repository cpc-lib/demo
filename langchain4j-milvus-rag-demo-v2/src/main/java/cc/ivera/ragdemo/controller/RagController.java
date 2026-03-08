package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.model.ChatAnswer;
import cc.ivera.ragdemo.service.KnowledgeIngestionService;
import cc.ivera.ragdemo.service.MilvusCollectionQueryService;
import cc.ivera.ragdemo.service.RagChatService;
import cc.ivera.ragdemo.service.vector.ActiveMilvusContext;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeIngestionService ingestionService;
    private final RagChatService ragChatService;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final MilvusCollectionQueryService milvusCollectionQueryService;

    @PostMapping("/ingest/text")
    public Map<String, Object> ingestText(@Valid @RequestBody IngestTextRequest req) {
        int chunks = ingestionService.ingestText(req.text());
        return Map.of(
                "ok", true,
                "chunks", chunks,
                "activeMilvus", dynamicMilvusStoreManager.currentAlias()
        );
    }

    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFile(@RequestPart("file") MultipartFile file) {
        int chunks = ingestionService.ingestFile(file);
        return Map.of(
                "ok", true,
                "chunks", chunks,
                "fileName", file.getOriginalFilename(),
                "activeMilvus", dynamicMilvusStoreManager.currentAlias()
        );
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam("question") String question,
                                 @RequestParam(value = "conversationId", required = false) String conversationId) {
        return ragChatService.streamAnswer(conversationId, question);
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question,
                       @RequestParam(value = "conversationId", required = false) String conversationId) {
        return ragChatService.answer(conversationId, question);
    }

    @GetMapping("/chat/detail")
    public ChatAnswer chatDetail(@RequestParam("question") String question,
                                 @RequestParam(value = "conversationId", required = false) String conversationId) {
        return ragChatService.answerDetailed(conversationId, question);
    }

    @GetMapping("/vector-stores/current")
    public Map<String, Object> currentVectorStore() {
        ActiveMilvusContext context = dynamicMilvusStoreManager.current();
        return Map.of(
                "ok", true,
                "activeAlias", context.alias(),
                "config", context.config()
        );
    }

    @GetMapping("/vector-stores")
    public List<?> listVectorStores() {
        return dynamicMilvusStoreManager.listAll();
    }

    @PostMapping("/vector-stores")
    public Map<String, Object> saveVectorStore(@Valid @RequestBody MilvusConfigRequest request) {
        dynamicMilvusStoreManager.saveOrUpdate(request.toStoreConfig());
        return Map.of(
                "ok", true,
                "alias", request.alias()
        );
    }

    @PostMapping("/vector-stores/switch")
    public Map<String, Object> switchVectorStore(@RequestParam("alias") String alias) {
        dynamicMilvusStoreManager.switchTo(alias);
        return Map.of(
                "ok", true,
                "activeAlias", alias
        );
    }

    @GetMapping("/milvus/collections")
    public Map<String, Object> listCollections(@RequestParam(value = "databaseName", required = false) String databaseName) throws InterruptedException {
        return Map.of(
                "ok", true,
                "databaseName", databaseName == null || databaseName.isBlank() ? "default" : databaseName,
                "collections", milvusCollectionQueryService.listCollections(databaseName)
        );
    }

    @GetMapping("/milvus/collections/{collectionName}")
    public Map<String, Object> describeCollection(@PathVariable String collectionName,
                                                  @RequestParam(value = "databaseName", required = false) String databaseName) throws InterruptedException {
        return Map.of(
                "ok", true,
                "data", milvusCollectionQueryService.describeCollection(databaseName, collectionName)
        );
    }

    @PostMapping("/milvus/collections/query")
    public Map<String, Object> queryCollection(@Valid @RequestBody MilvusCollectionQueryRequest request) throws InterruptedException {
        return Map.of(
                "ok", true,
                "data", milvusCollectionQueryService.query(request)
        );
    }

    @PostMapping("/milvus/collections")
    public Map<String, Object> createCollection(@Valid @RequestBody MilvusCreateCollectionRequest request) throws InterruptedException {
        return Map.of(
                "ok", true,
                "data", milvusCollectionQueryService.createCollection(request)
        );
    }
}
