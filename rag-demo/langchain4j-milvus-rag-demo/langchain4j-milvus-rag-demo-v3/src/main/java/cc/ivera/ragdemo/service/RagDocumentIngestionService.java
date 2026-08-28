package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.RagDocument;
import cc.ivera.ragdemo.domain.rag.RagDocumentVersion;
import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.domain.rag.RagKnowledgeBase;
import cc.ivera.ragdemo.mapper.RagDocumentMapper;
import cc.ivera.ragdemo.mapper.RagDocumentVersionMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.model.knowledge.RagIngestionSubmitResponse;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.quota.TenantQuotaService;
import cc.ivera.ragdemo.service.ragops.ObjectStorageService;
import cc.ivera.ragdemo.service.ragops.RagHashing;
import cc.ivera.ragdemo.service.ragops.StoredObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagDocumentIngestionService {

    private final RagKnowledgeBaseService knowledgeBaseService;
    private final RagDocumentVersionService documentVersionService;
    private final RagDocumentMapper documentMapper;
    private final RagDocumentVersionMapper versionMapper;
    private final RagIngestionTaskMapper taskMapper;
    private final ObjectStorageService objectStorageService;
    private final RagIngestionTaskPublisher taskPublisher;
    private final TenantQuotaService tenantQuotaService;

    public RagIngestionSubmitResponse submitToDefaultKnowledgeBase(MultipartFile file) {
        return submit(knowledgeBaseService.getOrCreateDefault().getId(), file);
    }

    public RagIngestionSubmitResponse submitTextToDefaultKnowledgeBase(String text, String fileName) {
        return submitText(knowledgeBaseService.getOrCreateDefault().getId(), text, fileName);
    }

    public RagIngestionSubmitResponse submitText(Long knowledgeBaseId, String text, String fileName) {
        if (!StringUtils.hasText(text)) {
            throw new IllegalArgumentException("Text content is required");
        }
        String resolvedFileName = StringUtils.hasText(fileName) ? fileName.trim() : "ingested-text.txt";
        return submitBytes(knowledgeBaseId, resolvedFileName, "text/plain", text.getBytes(StandardCharsets.UTF_8));
    }

    public RagIngestionSubmitResponse submit(Long knowledgeBaseId, MultipartFile file) {
        try {
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : "unknown";
            return submitBytes(knowledgeBaseId, fileName, file.getContentType(), file.getBytes());
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit RAG ingestion task: " + e.getMessage(), e);
        }
    }

    private RagIngestionSubmitResponse submitBytes(Long knowledgeBaseId, String fileName, String mimeType, byte[] bytes) {
        try {
            RagKnowledgeBase kb = knowledgeBaseService.getRequired(knowledgeBaseId);
            tenantQuotaService.assertFileUploadAllowed(bytes.length);
            String fileHash = RagHashing.sha256Hex(bytes);
            String idempotencyKey = "tenant:%s:%s".formatted(kb.getTenantId(),
                    RagHashing.ingestionIdempotencyKey(kb.getId(), fileName, fileHash));

            RagIngestionTask existingTask = taskMapper.selectOne(new LambdaQueryWrapper<RagIngestionTask>()
                    .eq(RagIngestionTask::getIdempotencyKey, idempotencyKey)
                    .last("LIMIT 1"));
            if (existingTask != null) {
                RagDocument document = documentMapper.selectById(existingTask.getDocumentId());
                RagDocumentVersion version = existingTask.getDocumentVersionId() == null
                        ? null
                        : versionMapper.selectById(existingTask.getDocumentVersionId());
                return documentVersionService.toSubmitResponse(document, version, existingTask, fileHash);
            }

            String documentUid = "doc_" + UUID.randomUUID().toString().replace("-", "");
            StoredObject object = objectStorageService.saveOriginal(kb.getTenantId(), kb.getId(), documentUid, fileHash, fileName, bytes);
            RagDocument document = createDocument(kb, documentUid, fileName, mimeType, fileHash, object);
            documentMapper.insert(document);
            RagDocumentVersion version = documentVersionService.createInitialVersion(kb, document);

            RagIngestionTask task = documentVersionService.createParseTask(document, version, idempotencyKey);

            taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(kb.getTenantId(), task.getId(), document.getId(), kb.getId(), version.getId(), task.getTaskNo()));
            return documentVersionService.toSubmitResponse(document, version, task, fileHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to submit RAG ingestion task: " + e.getMessage(), e);
        }
    }

    private RagDocument createDocument(RagKnowledgeBase kb,
                                       String documentUid,
                                       String fileName,
                                       String mimeType,
                                       String fileHash,
                                       StoredObject object) {
        RagDocument document = new RagDocument();
        document.setTenantId(kb.getTenantId());
        document.setKnowledgeBaseId(kb.getId());
        document.setDocumentUid(documentUid);
        document.setDocumentName(fileName);
        document.setSourceType(1);
        document.setSourceUri(object.uri());
        document.setObjectKey(object.objectKey());
        document.setOriginalFilename(fileName);
        document.setFileExtension(extensionOf(fileName));
        document.setMimeType(mimeType);
        document.setFileSize(object.size());
        document.setFileHash(fileHash);
        document.setCurrentVersionNo(1);
        document.setChunkCount(0);
        document.setCharacterCount(0L);
        document.setTokenCount(0L);
        document.setParseStatus(0);
        document.setChunkStatus(0);
        document.setEmbeddingStatus(0);
        document.setDocumentStatus(0);
        document.setMetadataJson("{}");
        document.setIsDeleted(0);
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        return document;
    }

    private String extensionOf(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }
}
