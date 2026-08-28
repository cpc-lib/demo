package cc.ivera.ragdemo.service.ragops;

import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.TenantScopedObjectKeyFactory;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * MinIO-backed implementation of {@link ObjectStorageService}.
 * <p>
 * Activated when {@code rag.object-storage.type=minio}.
 * Uses the MinIO Java SDK to store and retrieve objects.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "rag.object-storage.type", havingValue = "minio")
@RequiredArgsConstructor(onConstructor_ = {@org.springframework.beans.factory.annotation.Autowired})
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;
    private final RagProperties properties;
    private final TenantScopedObjectKeyFactory objectKeyFactory;

    @Override
    public StoredObject save(String fileHash, String fileName, byte[] bytes) {
        String safeFileName = sanitize(fileName);
        String objectKey = fileHash.substring(0, 2) + "/" + fileHash + "/" + safeFileName;
        return putObject(objectKey, bytes);
    }

    @Override
    public StoredObject saveOriginal(Long tenantId, Long knowledgeBaseId, String documentRef,
                                     String fileHash, String fileName, byte[] bytes) {
        String objectKey = objectKeyFactory.originalObjectKey(
                properties.getTenant().getObjectEnvironment(),
                tenantId,
                knowledgeBaseId,
                documentRef,
                fileHash,
                fileName
        );
        return putObject(objectKey, bytes);
    }

    @Override
    public StoredObject saveVersion(Long tenantId, Long knowledgeBaseId, String documentRef,
                                    Integer versionNo, String fileHash, String fileName, byte[] bytes) {
        String objectKey = objectKeyFactory.versionObjectKey(
                properties.getTenant().getObjectEnvironment(),
                tenantId,
                knowledgeBaseId,
                documentRef,
                versionNo,
                fileHash,
                fileName
        );
        return putObject(objectKey, bytes);
    }

    private StoredObject putObject(String objectKey, byte[] bytes) {
        String bucket = properties.getObjectStorage().getBucket();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(new ByteArrayInputStream(bytes != null ? bytes : new byte[0]),
                                    bytes != null ? bytes.length : 0, -1)
                            .build()
            );
            String uri = "minio://" + bucket + "/" + objectKey;
            return new StoredObject(objectKey, uri, bytes != null ? bytes.length : 0);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to save object to MinIO: " + objectKey, e);
        }
    }

    @Override
    public byte[] read(String objectKey) {
        String bucket = properties.getObjectStorage().getBucket();
        try (InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to read object from MinIO: " + objectKey, e);
        }
    }

    @Override
    public boolean deleteIfExists(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        String bucket = properties.getObjectStorage().getBucket();
        try {
            minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long countTenantObjects(String environment, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String bucket = properties.getObjectStorage().getBucket();
        String prefix = (StringUtils.hasText(environment) ? environment : "default") + "/" + tenantId + "/";
        long count = 0;
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    count++;
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to count tenant objects: " + tenantId, e);
        }
        return count;
    }

    @Override
    public long deleteTenantObjects(String environment, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        String bucket = properties.getObjectStorage().getBucket();
        String prefix = (StringUtils.hasText(environment) ? environment : "default") + "/" + tenantId + "/";
        long deleted = 0;
        try {
            List<String> objectKeys = new ArrayList<>();
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(bucket)
                            .prefix(prefix)
                            .recursive(true)
                            .build()
            );
            for (Result<Item> result : results) {
                Item item = result.get();
                if (!item.isDir()) {
                    objectKeys.add(item.objectName());
                }
            }
            for (String key : objectKeys) {
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build());
                deleted++;
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to delete tenant objects: " + tenantId, e);
        }
        return deleted;
    }

    private String sanitize(String fileName) {
        String value = StringUtils.hasText(fileName) ? fileName.trim() : "unknown";
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
