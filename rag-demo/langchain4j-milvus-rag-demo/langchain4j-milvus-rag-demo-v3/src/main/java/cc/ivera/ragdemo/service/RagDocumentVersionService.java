package cc.ivera.ragdemo.service;

import cc.ivera.ragdemo.audit.TenantAuditService;
import cc.ivera.ragdemo.domain.rag.*;
import cc.ivera.ragdemo.mapper.RagDocumentChunkMapper;
import cc.ivera.ragdemo.mapper.RagDocumentMapper;
import cc.ivera.ragdemo.mapper.RagDocumentVersionMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.model.knowledge.*;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.permission.KnowledgeBasePermissionService;
import cc.ivera.ragdemo.quota.TenantQuotaService;
import cc.ivera.ragdemo.service.query.KeywordSearchIndex;
import cc.ivera.ragdemo.service.ragops.*;
import cc.ivera.ragdemo.service.vector.DynamicMilvusStoreManager;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagDocumentVersionService {

    private static final int DEFAULT_PAGE_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 500;
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final RagDocumentMapper documentMapper;
    private final RagDocumentVersionMapper versionMapper;
    private final RagIngestionTaskMapper taskMapper;
    private final RagDocumentChunkMapper chunkMapper;
    private final ObjectStorageService objectStorageService;
    private final RagIngestionTaskPublisher taskPublisher;
    private final DynamicMilvusStoreManager dynamicMilvusStoreManager;
    private final KeywordSearchIndex keywordSearchIndex;
    private final ObjectMapper objectMapper;
    private final DocumentTextDiff documentTextDiff;
    private final DocumentVersionReviewPolicy reviewPolicy;
    private KnowledgeBasePermissionService permissionService;
    private TenantQuotaService tenantQuotaService;
    private TenantAuditService auditService;

    @Autowired(required = false)
    public void setPermissionService(KnowledgeBasePermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Autowired(required = false)
    public void setTenantQuotaService(TenantQuotaService tenantQuotaService) {
        this.tenantQuotaService = tenantQuotaService;
    }

    @Autowired(required = false)
    public void setAuditService(TenantAuditService auditService) {
        this.auditService = auditService;
    }

    public RagDocumentVersion getRequiredVersion(Long versionId) {
        RagDocumentVersion version = versionMapper.selectById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("Document version not found: " + versionId);
        }
        assertVersionTenant(version);
        return version;
    }

    public RagDocumentVersion getRequiredVersion(Long documentId, Integer versionNo) {
        getRequiredDocument(documentId);
        RagDocumentVersion version = versionMapper.selectOne(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId)
                .eq(RagDocumentVersion::getVersionNo, versionNo)
                .eq(RagDocumentVersion::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (version == null) {
            throw new IllegalArgumentException("Document version not found: " + documentId + " v" + versionNo);
        }
        assertVersionTenant(version);
        return version;
    }

    public RagDocumentVersion currentVersion(RagDocument document) {
        if (document.getCurrentVersionId() != null) {
            RagDocumentVersion version = versionMapper.selectById(document.getCurrentVersionId());
            if (version != null) {
                return version;
            }
        }
        RagDocumentVersion version = versionMapper.selectOne(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, document.getId())
                .eq(RagDocumentVersion::getCurrentFlag, true)
                .eq(RagDocumentVersion::getIsDeleted, 0)
                .last("LIMIT 1"));
        if (version == null) {
            throw new IllegalArgumentException("Current document version not found: " + document.getId());
        }
        return version;
    }

    public List<RagDocumentVersion> listVersions(Long documentId) {
        getRequiredDocument(documentId);
        return versionMapper.selectList(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId)
                .eq(RagDocumentVersion::getIsDeleted, 0)
                .orderByDesc(RagDocumentVersion::getVersionNo));
    }

    public PageResponse<RagDocumentVersion> pageVersions(Long documentId, PageQuery pageQuery) {
        getRequiredDocument(documentId);
        PageQuery query = normalizePageQuery(pageQuery);
        long total = versionMapper.selectCount(versionListQuery(documentId));
        LambdaQueryWrapper<RagDocumentVersion> rowsQuery = versionListQuery(documentId);
        applyOrder(rowsQuery, query);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, versionMapper.selectList(rowsQuery));
    }

    @Transactional
    public RagDocumentVersion createInitialVersion(RagKnowledgeBase kb, RagDocument document) {
        RagDocumentVersion version = buildVersionFromDocument(kb.getTenantId(), kb.getId(), document, 1, true);
        versionMapper.insert(version);
        applyVersionToDocument(document, version);
        documentMapper.updateById(document);
        return version;
    }

    @Transactional
    public RagIngestionSubmitResponse replace(Long documentId, MultipartFile file) {
        try {
            RagDocument document = getRequiredDocument(documentId);
            requireEditor(document);
            String fileName = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : document.getDocumentName();
            byte[] bytes = file.getBytes();
            if (tenantQuotaService != null) {
                tenantQuotaService.assertFileUploadAllowed(bytes.length);
            }
            String fileHash = RagHashing.sha256Hex(bytes);
            int nextVersionNo = nextVersionNo(documentId);
            StoredObject object = objectStorageService.saveVersion(
                    document.getTenantId(),
                    document.getKnowledgeBaseId(),
                    document.getDocumentUid(),
                    nextVersionNo,
                    fileHash,
                    fileName,
                    bytes
            );
            RagDocumentVersion version = buildVersion(
                    document.getTenantId(),
                    document.getKnowledgeBaseId(),
                    document.getId(),
                    nextVersionNo,
                    fileName,
                    1,
                    object.uri(),
                    object.objectKey(),
                    fileName,
                    extensionOf(fileName),
                    file.getContentType(),
                    object.size(),
                    fileHash,
                    true
            );
            clearCurrentVersions(document.getId());
            versionMapper.insert(version);
            applyVersionToDocument(document, version);
            markDocumentProcessing(document);
            documentMapper.updateById(document);
            RagIngestionTask task = createParseTask(document, version, "replace:" + document.getId() + ":" + version.getVersionNo() + ":" + fileHash);
            taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(document.getTenantId(), task.getId(), document.getId(), document.getKnowledgeBaseId(), version.getId(), task.getTaskNo()));
            recordAudit("DOCUMENT_REPLACE", document.getId());
            return toSubmitResponse(document, version, task, fileHash);
        } catch (Exception e) {
            throw new RuntimeException("Failed to replace RAG document: " + e.getMessage(), e);
        }
    }

    @Transactional
    public RagIngestionSubmitResponse reparse(Long documentId) {
        RagDocument document = getRequiredDocument(documentId);
        requireEditor(document);
        RagDocumentVersion version = currentVersion(document);
        resetVersionForProcessing(version);
        versionMapper.updateById(version);
        markDocumentProcessing(document);
        documentMapper.updateById(document);
        RagIngestionTask task = createParseTask(document, version,
                "reparse:" + document.getId() + ":" + version.getId() + ":" + UUID.randomUUID().toString().replace("-", ""));
        taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(document.getTenantId(), task.getId(), document.getId(), document.getKnowledgeBaseId(), version.getId(), task.getTaskNo()));
        recordAudit("DOCUMENT_REPARSE", document.getId());
        return toSubmitResponse(document, version, task, version.getFileHash());
    }

    @Transactional
    public RagIngestionSubmitResponse rollback(Long documentId, Integer versionNo) {
        RagDocument document = getRequiredDocument(documentId);
        requireEditor(document);
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        DocumentVersionPolicy.assertCanBecomeCurrent(DocumentVersionStatus.fromCode(version.getVersionStatus()));
        clearCurrentVersions(documentId);
        version.setCurrentFlag(true);
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        applyVersionToDocument(document, version);
        markDocumentProcessing(document);
        documentMapper.updateById(document);
        RagIngestionTask task = createParseTask(document, version,
                "rollback:" + document.getId() + ":" + version.getId() + ":" + UUID.randomUUID().toString().replace("-", ""));
        taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(document.getTenantId(), task.getId(), document.getId(), document.getKnowledgeBaseId(), version.getId(), task.getTaskNo()));
        recordAudit("DOCUMENT_ROLLBACK", document.getId());
        return toSubmitResponse(document, version, task, version.getFileHash());
    }

    @Transactional
    public RagDocument disable(Long documentId) {
        RagDocument document = getRequiredDocument(documentId);
        requireAdmin(document);
        document.setDocumentStatus(3);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        updateCurrentVersionStatus(document, DocumentVersionStatus.DISABLED);
        recordAudit("DOCUMENT_DISABLE", document.getId());
        return document;
    }

    @Transactional
    public RagDocument enable(Long documentId) {
        RagDocument document = getRequiredDocument(documentId);
        requireAdmin(document);
        RagDocumentVersion version = currentVersion(document);
        version.setVersionStatus(DocumentVersionStatus.AVAILABLE.code());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        document.setDocumentStatus(1);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        recordAudit("DOCUMENT_ENABLE", document.getId());
        return document;
    }

    @Transactional
    public RagDocumentDeleteResponse delete(Long documentId) {
        RagDocument document = getRequiredDocument(documentId);
        requireAdmin(document);
        List<RagDocumentVersion> versions = listAllVersionsIncludingDeleted(documentId);
        List<RagDocumentChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<RagDocumentChunk>()
                .eq(RagDocumentChunk::getDocumentId, documentId));
        int deletedVectorCount = cleanupVectors(chunks);
        cleanupKeywordIndex(chunks);
        int deletedObjectCount = cleanupObjectFiles(document, versions);
        document.setIsDeleted(1);
        document.setDocumentStatus(3);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        versionMapper.update(null, new UpdateWrapper<RagDocumentVersion>()
                .eq("document_id", documentId)
                .set("version_status", DocumentVersionStatus.DELETED.code())
                .set("is_deleted", 1)
                .set("is_current", false)
                .set("updated_at", LocalDateTime.now()));
        int deletedChunkRows = cleanupChunkRows(documentId);
        recordAudit("DOCUMENT_DELETE", document.getId());
        return new RagDocumentDeleteResponse(document, deletedVectorCount, deletedObjectCount, deletedChunkRows);
    }

    public RagDocumentVersionDiffResponse diff(Long documentId, Integer leftVersionNo, Integer rightVersionNo) {
        RagDocumentVersion left = getRequiredVersion(documentId, leftVersionNo);
        RagDocumentVersion right = getRequiredVersion(documentId, rightVersionNo);
        String leftText = new String(objectStorageService.read(left.getObjectKey()), StandardCharsets.UTF_8);
        String rightText = new String(objectStorageService.read(right.getObjectKey()), StandardCharsets.UTF_8);
        return documentTextDiff.diff(documentId, leftVersionNo, rightVersionNo, leftText, rightText);
    }

    @Transactional
    public RagDocumentVersion updateNote(Long documentId, Integer versionNo, RagDocumentVersionNoteRequest request) {
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        requireEditorForVersion(version);
        version.setVersionNote(request == null ? null : truncate(request.note(), 2000));
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        recordAudit("DOCUMENT_VERSION_NOTE", version.getId());
        return version;
    }

    @Transactional
    public RagDocumentVersion submitReview(Long documentId, Integer versionNo, RagDocumentVersionReviewRequest request) {
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        requireAdminForVersion(version);
        reviewPolicy.assertCanSubmit(version.getApprovalStatus());
        version.setApprovalStatus(DocumentVersionReviewPolicy.PENDING_REVIEW);
        version.setApprovalComment(truncate(request == null ? null : request.comment(), 2000));
        version.setApprovedBy(normalizeOperator(request == null ? null : request.operator()));
        version.setApprovedAt(null);
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        recordAudit("DOCUMENT_VERSION_SUBMIT_REVIEW", version.getId());
        return version;
    }

    @Transactional
    public RagDocumentVersion approve(Long documentId, Integer versionNo, RagDocumentVersionReviewRequest request) {
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        requireAdminForVersion(version);
        reviewPolicy.assertCanApproveOrReject(version.getApprovalStatus());
        version.setApprovalStatus(DocumentVersionReviewPolicy.APPROVED);
        version.setApprovalComment(truncate(request == null ? null : request.comment(), 2000));
        version.setApprovedBy(normalizeOperator(request == null ? null : request.operator()));
        version.setApprovedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        recordAudit("DOCUMENT_VERSION_APPROVE", version.getId());
        return version;
    }

    @Transactional
    public RagDocumentVersion reject(Long documentId, Integer versionNo, RagDocumentVersionReviewRequest request) {
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        requireAdminForVersion(version);
        reviewPolicy.assertCanApproveOrReject(version.getApprovalStatus());
        version.setApprovalStatus(DocumentVersionReviewPolicy.REJECTED);
        version.setApprovalComment(truncate(request == null ? null : request.comment(), 2000));
        version.setApprovedBy(normalizeOperator(request == null ? null : request.operator()));
        version.setApprovedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        recordAudit("DOCUMENT_VERSION_REJECT", version.getId());
        return version;
    }

    @Transactional
    public RagDocumentVersion publish(Long documentId, Integer versionNo, RagDocumentVersionReviewRequest request) {
        RagDocument document = getRequiredDocument(documentId);
        requireAdmin(document);
        RagDocumentVersion version = getRequiredVersion(documentId, versionNo);
        reviewPolicy.assertCanPublish(version.getApprovalStatus());
        if (!Boolean.TRUE.equals(version.getCurrentFlag())) {
            throw new IllegalStateException("Only current document version can be published safely; rollback or reparse it first");
        }
        DocumentVersionPolicy.assertCanBecomeCurrent(DocumentVersionStatus.fromCode(version.getVersionStatus()));
        version.setApprovalStatus(DocumentVersionReviewPolicy.PUBLISHED);
        version.setApprovalComment(truncate(request == null ? version.getApprovalComment() : request.comment(), 2000));
        version.setApprovedBy(normalizeOperator(request == null ? version.getApprovedBy() : request.operator()));
        version.setPublishedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
        document.setDocumentStatus(1);
        document.setUpdatedAt(LocalDateTime.now());
        documentMapper.updateById(document);
        recordAudit("DOCUMENT_VERSION_PUBLISH", version.getId());
        return version;
    }

    public RagDocumentBatchResponse batch(RagDocumentBatchRequest request) {
        String operation = request == null ? "" : normalizeOperation(request.operation());
        List<Long> ids = request == null || request.documentIds() == null
                ? List.of()
                : request.documentIds().stream().filter(id -> id != null && id > 0).distinct().toList();
        List<RagDocumentBatchItemResult> results = new ArrayList<>();
        for (Long documentId : ids) {
            try {
                switch (operation) {
                    case "DELETE" -> delete(documentId);
                    case "DISABLE" -> disable(documentId);
                    case "ENABLE" -> enable(documentId);
                    case "REPARSE" -> reparse(documentId);
                    default -> throw new IllegalArgumentException("Unsupported document batch operation: " + operation);
                }
                results.add(new RagDocumentBatchItemResult(documentId, true, "OK"));
            } catch (Exception e) {
                results.add(new RagDocumentBatchItemResult(documentId, false, e.getMessage()));
            }
        }
        int succeeded = (int) results.stream().filter(RagDocumentBatchItemResult::success).count();
        return new RagDocumentBatchResponse(operation, ids.size(), succeeded, ids.size() - succeeded, results);
    }

    public RagDocumentVersionDownload download(Long documentId, Integer versionNo) {
        RagDocument document = getRequiredDocument(documentId);
        RagDocumentVersion version = versionNo == null ? currentVersion(document) : getRequiredVersion(documentId, versionNo);
        if (!StringUtils.hasText(version.getObjectKey())) {
            throw new IllegalArgumentException("Document version has no object key: " + version.getId());
        }
        return new RagDocumentVersionDownload(version, objectStorageService.read(version.getObjectKey()));
    }

    public RagIngestionTask createParseTask(RagDocument document,
                                            RagDocumentVersion version,
                                            String idempotencyKey) {
        RagIngestionTask task = new RagIngestionTask();
        task.setTenantId(document.getTenantId());
        task.setKnowledgeBaseId(document.getKnowledgeBaseId());
        task.setDocumentId(document.getId());
        task.setDocumentVersionId(version.getId());
        task.setTaskNo("ing_" + UUID.randomUUID().toString().replace("-", ""));
        task.setTaskType(IngestionTaskType.PARSE.name());
        task.setTaskStatus(IngestionTaskStatus.PENDING.code());
        task.setProgress(0);
        task.setTotalCount(0);
        task.setSuccessCount(0);
        task.setFailedCount(0);
        task.setRetryCount(0);
        task.setMaxRetryCount(3);
        task.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        task.setIdempotencyKey(idempotencyKey);
        task.setLockVersion(0L);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.insert(task);
        return task;
    }

    public RagIngestionSubmitResponse toSubmitResponse(RagDocument document,
                                                       RagDocumentVersion version,
                                                       RagIngestionTask task,
                                                       String fileHash) {
        return new RagIngestionSubmitResponse(
                document.getKnowledgeBaseId(),
                document.getId(),
                version == null ? null : version.getId(),
                version == null ? null : version.getVersionNo(),
                task.getId(),
                task.getTaskNo(),
                document.getDocumentUid(),
                version == null ? document.getObjectKey() : version.getObjectKey(),
                fileHash,
                IngestionTaskStatus.fromCode(task.getTaskStatus()).name()
        );
    }

    public RagDocument getRequiredDocument(Long documentId) {
        RagDocument document = documentMapper.selectById(documentId);
        if (document == null || Integer.valueOf(1).equals(document.getIsDeleted())) {
            throw new IllegalArgumentException("Document not found: " + documentId);
        }
        TenantContextHolder.currentTenantId().ifPresent(currentTenant -> {
            if (!currentTenant.equals(document.getTenantId())) {
                throw new cc.ivera.ragdemo.exception.TenantAccessDeniedException("Document belongs to another tenant");
            }
        });
        requireRead(document);
        return document;
    }

    private List<RagDocumentVersion> listAllVersionsIncludingDeleted(Long documentId) {
        return versionMapper.selectList(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId));
    }

    private int cleanupVectors(List<RagDocumentChunk> chunks) {
        Map<String, List<String>> idsByAlias = new LinkedHashMap<>();
        for (RagDocumentChunk chunk : chunks) {
            List<String> ids = vectorIds(chunk);
            if (ids.isEmpty()) {
                continue;
            }
            String alias = StringUtils.hasText(chunk.getMilvusAlias())
                    ? chunk.getMilvusAlias()
                    : dynamicMilvusStoreManager.currentAlias();
            idsByAlias.computeIfAbsent(alias, ignored -> new ArrayList<>()).addAll(ids);
        }
        int removed = 0;
        for (Map.Entry<String, List<String>> entry : idsByAlias.entrySet()) {
            List<String> ids = entry.getValue().stream().distinct().toList();
            if (!ids.isEmpty()) {
                dynamicMilvusStoreManager.context(entry.getKey()).store().removeAll(ids);
                removed += ids.size();
            }
        }
        return removed;
    }

    private void cleanupKeywordIndex(List<RagDocumentChunk> chunks) {
        if (keywordSearchIndex == null || !keywordSearchIndex.enabled()) {
            return;
        }
        chunks.stream()
                .map(RagDocumentChunk::getChunkUid)
                .filter(StringUtils::hasText)
                .distinct()
                .forEach(keywordSearchIndex::delete);
    }

    private int cleanupObjectFiles(RagDocument document, List<RagDocumentVersion> versions) {
        Set<String> objectKeys = new LinkedHashSet<>();
        if (StringUtils.hasText(document.getObjectKey())) {
            objectKeys.add(document.getObjectKey());
        }
        versions.stream()
                .map(RagDocumentVersion::getObjectKey)
                .filter(StringUtils::hasText)
                .forEach(objectKeys::add);
        int deleted = 0;
        for (String objectKey : objectKeys) {
            if (!objectKeyReferencedByOtherDocuments(objectKey, document.getId())
                    && objectStorageService.deleteIfExists(objectKey)) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean objectKeyReferencedByOtherDocuments(String objectKey, Long documentId) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        Long documentRefs = documentMapper.selectCount(new LambdaQueryWrapper<RagDocument>()
                .eq(RagDocument::getObjectKey, objectKey)
                .ne(RagDocument::getId, documentId)
                .eq(RagDocument::getIsDeleted, 0));
        Long versionRefs = versionMapper.selectCount(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getObjectKey, objectKey)
                .ne(RagDocumentVersion::getDocumentId, documentId)
                .eq(RagDocumentVersion::getIsDeleted, 0));
        return safeCount(documentRefs) + safeCount(versionRefs) > 0;
    }

    private int cleanupChunkRows(Long documentId) {
        return chunkMapper.update(null, new UpdateWrapper<RagDocumentChunk>()
                .eq("document_id", documentId)
                .set("chunk_status", "DELETED")
                .set("is_current", false)
                .set("is_deleted", 1)
                .set("updated_at", LocalDateTime.now()));
    }

    private List<String> vectorIds(RagDocumentChunk chunk) {
        Set<String> ids = new LinkedHashSet<>();
        if (StringUtils.hasText(chunk.getVectorId())) {
            ids.add(chunk.getVectorId());
        }
        ids.addAll(readStringList(chunk.getTextVectorIds()));
        ids.addAll(readStringList(chunk.getImageVectorIds()));
        return ids.stream().filter(StringUtils::hasText).toList();
    }

    private List<String> readStringList(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (Exception e) {
            log.warn("Failed to parse string list JSON, returning empty list: {}", e.getMessage());
            return List.of();
        }
    }

    private RagDocumentVersion buildVersionFromDocument(Long tenantId,
                                                       Long knowledgeBaseId,
                                                       RagDocument document,
                                                       Integer versionNo,
                                                       boolean current) {
        return buildVersion(
                tenantId,
                knowledgeBaseId,
                document.getId(),
                versionNo,
                document.getDocumentName(),
                document.getSourceType(),
                document.getSourceUri(),
                document.getObjectKey(),
                document.getOriginalFilename(),
                document.getFileExtension(),
                document.getMimeType(),
                document.getFileSize(),
                document.getFileHash(),
                current
        );
    }

    private RagDocumentVersion buildVersion(Long tenantId,
                                            Long knowledgeBaseId,
                                            Long documentId,
                                            Integer versionNo,
                                            String documentName,
                                            Integer sourceType,
                                            String sourceUri,
                                            String objectKey,
                                            String originalFilename,
                                            String fileExtension,
                                            String mimeType,
                                            Long fileSize,
                                            String fileHash,
                                            boolean current) {
        RagDocumentVersion version = new RagDocumentVersion();
        version.setTenantId(tenantId);
        version.setKnowledgeBaseId(knowledgeBaseId);
        version.setDocumentId(documentId);
        version.setVersionNo(versionNo);
        version.setVersionUid("ver_" + UUID.randomUUID().toString().replace("-", ""));
        version.setDocumentName(documentName);
        version.setSourceType(sourceType == null ? 1 : sourceType);
        version.setSourceUri(sourceUri);
        version.setObjectKey(objectKey);
        version.setOriginalFilename(originalFilename);
        version.setFileExtension(fileExtension);
        version.setMimeType(mimeType);
        version.setFileSize(fileSize);
        version.setFileHash(fileHash);
        version.setChunkCount(0);
        version.setCharacterCount(0L);
        version.setTokenCount(0L);
        version.setParseStatus(0);
        version.setChunkStatus(0);
        version.setEmbeddingStatus(0);
        version.setVersionStatus(DocumentVersionStatus.PROCESSING.code());
        version.setCurrentFlag(current);
        version.setApprovalStatus(DocumentVersionReviewPolicy.DRAFT);
        version.setMetadataJson("{}");
        version.setIsDeleted(0);
        version.setCreatedAt(LocalDateTime.now());
        version.setUpdatedAt(LocalDateTime.now());
        return version;
    }

    private int nextVersionNo(Long documentId) {
        RagDocumentVersion latest = versionMapper.selectOne(new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId)
                .orderByDesc(RagDocumentVersion::getVersionNo)
                .last("LIMIT 1"));
        return DocumentVersionPolicy.nextVersionNo(latest == null ? null : latest.getVersionNo());
    }

    private void clearCurrentVersions(Long documentId) {
        versionMapper.update(null, new LambdaUpdateWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId)
                .set(RagDocumentVersion::getCurrentFlag, false)
                .set(RagDocumentVersion::getUpdatedAt, LocalDateTime.now()));
    }

    private void applyVersionToDocument(RagDocument document, RagDocumentVersion version) {
        document.setCurrentVersionId(version.getId());
        document.setCurrentVersionNo(version.getVersionNo());
        document.setDocumentName(version.getDocumentName());
        document.setSourceType(version.getSourceType());
        document.setSourceUri(version.getSourceUri());
        document.setObjectKey(version.getObjectKey());
        document.setOriginalFilename(version.getOriginalFilename());
        document.setFileExtension(version.getFileExtension());
        document.setMimeType(version.getMimeType());
        document.setFileSize(version.getFileSize());
        document.setFileHash(version.getFileHash());
        document.setUpdatedAt(LocalDateTime.now());
    }

    private void markDocumentProcessing(RagDocument document) {
        document.setParseStatus(0);
        document.setChunkStatus(0);
        document.setEmbeddingStatus(0);
        document.setDocumentStatus(0);
        document.setErrorCode(null);
        document.setErrorMessage(null);
        document.setUpdatedAt(LocalDateTime.now());
    }

    private void resetVersionForProcessing(RagDocumentVersion version) {
        version.setParseStatus(0);
        version.setChunkStatus(0);
        version.setEmbeddingStatus(0);
        version.setVersionStatus(DocumentVersionStatus.PROCESSING.code());
        version.setErrorCode(null);
        version.setErrorMessage(null);
        version.setUpdatedAt(LocalDateTime.now());
    }

    private void updateCurrentVersionStatus(RagDocument document, DocumentVersionStatus status) {
        RagDocumentVersion version = currentVersion(document);
        version.setVersionStatus(status.code());
        version.setUpdatedAt(LocalDateTime.now());
        versionMapper.updateById(version);
    }

    private String extensionOf(String fileName) {
        int index = fileName == null ? -1 : fileName.lastIndexOf('.');
        return index < 0 ? "" : fileName.substring(index + 1).toLowerCase();
    }

    private String normalizeOperator(String operator) {
        return StringUtils.hasText(operator) ? operator.trim() : "system";
    }

    private String normalizeOperation(String operation) {
        return StringUtils.hasText(operation) ? operation.trim().toUpperCase(Locale.ROOT) : "";
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    private void requireRead(RagDocument document) {
        if (permissionService != null) {
            RagKnowledgeBase kb = new RagKnowledgeBase();
            kb.setId(document.getKnowledgeBaseId());
            kb.setTenantId(document.getTenantId());
            permissionService.requireRead(kb);
        }
    }

    private void requireEditor(RagDocument document) {
        if (permissionService != null) {
            RagKnowledgeBase kb = new RagKnowledgeBase();
            kb.setId(document.getKnowledgeBaseId());
            kb.setTenantId(document.getTenantId());
            permissionService.requireEditor(kb);
        }
    }

    private void requireAdmin(RagDocument document) {
        if (permissionService != null) {
            RagKnowledgeBase kb = new RagKnowledgeBase();
            kb.setId(document.getKnowledgeBaseId());
            kb.setTenantId(document.getTenantId());
            permissionService.requireAdmin(kb);
        }
    }

    private void requireAdminForVersion(RagDocumentVersion version) {
        if (permissionService != null) {
            RagKnowledgeBase kb = new RagKnowledgeBase();
            kb.setId(version.getKnowledgeBaseId());
            kb.setTenantId(version.getTenantId());
            permissionService.requireAdmin(kb);
        }
    }

    private void requireEditorForVersion(RagDocumentVersion version) {
        if (permissionService != null) {
            RagKnowledgeBase kb = new RagKnowledgeBase();
            kb.setId(version.getKnowledgeBaseId());
            kb.setTenantId(version.getTenantId());
            permissionService.requireEditor(kb);
        }
    }

    private void assertVersionTenant(RagDocumentVersion version) {
        TenantContextHolder.currentTenantId().ifPresent(currentTenant -> {
            if (!currentTenant.equals(version.getTenantId())) {
                throw new cc.ivera.ragdemo.exception.TenantAccessDeniedException("Document version belongs to another tenant");
            }
        });
    }

    private void recordAudit(String operation, Object resourceId) {
        if (auditService != null) {
            auditService.recordSuccess(operation, "RAG_DOCUMENT", resourceId);
        }
    }

    private LambdaQueryWrapper<RagDocumentVersion> versionListQuery(Long documentId) {
        return new LambdaQueryWrapper<RagDocumentVersion>()
                .eq(RagDocumentVersion::getDocumentId, documentId)
                .eq(RagDocumentVersion::getIsDeleted, 0);
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery) {
        PageQuery query = pageQuery == null
                ? PageQuery.of(1, null, DEFAULT_PAGE_SIZE, "versionNo", "DESC", MAX_PAGE_SIZE)
                : pageQuery;
        return query.withDefaultSort("versionNo", "DESC");
    }

    private void applyOrder(LambdaQueryWrapper<RagDocumentVersion> wrapper, PageQuery pageQuery) {
        boolean asc = pageQuery.ascending();
        switch (pageQuery.sortBy()) {
            case "id" -> wrapper.orderBy(true, asc, RagDocumentVersion::getId);
            case "versionNo" -> wrapper.orderBy(true, asc, RagDocumentVersion::getVersionNo);
            case "createdAt" -> wrapper.orderBy(true, asc, RagDocumentVersion::getCreatedAt);
            case "updatedAt" -> wrapper.orderBy(true, asc, RagDocumentVersion::getUpdatedAt);
            case "fileSize" -> wrapper.orderBy(true, asc, RagDocumentVersion::getFileSize);
            case "versionStatus" -> wrapper.orderBy(true, asc, RagDocumentVersion::getVersionStatus);
            default -> wrapper.orderByDesc(RagDocumentVersion::getVersionNo);
        }
    }
}
