package cc.ivera.ragdemo.service.ragops;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.tenant.TenantScopedObjectKeyFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

@Service
@ConditionalOnProperty(name = "rag.object-storage.type", havingValue = "local", matchIfMissing = true)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class LocalObjectStorageService implements ObjectStorageService {

    private final RagProperties properties;
    private final TenantScopedObjectKeyFactory objectKeyFactory;

    public StoredObject save(String fileHash, String fileName, byte[] bytes) {
        String safeFileName = sanitize(fileName);
        String objectKey = Path.of(fileHash.substring(0, 2), fileHash, safeFileName).toString().replace("\\", "/");
        return saveObjectKey(objectKey, bytes);
    }

    public StoredObject saveOriginal(Long tenantId,
                                     Long knowledgeBaseId,
                                     String documentRef,
                                     String fileHash,
                                     String fileName,
                                     byte[] bytes) {
        String objectKey = objectKeyFactory.originalObjectKey(
                properties.getTenant().getObjectEnvironment(),
                tenantId,
                knowledgeBaseId,
                documentRef,
                fileHash,
                fileName
        );
        return saveObjectKey(objectKey, bytes);
    }

    public StoredObject saveVersion(Long tenantId,
                                    Long knowledgeBaseId,
                                    String documentRef,
                                    Integer versionNo,
                                    String fileHash,
                                    String fileName,
                                    byte[] bytes) {
        String objectKey = objectKeyFactory.versionObjectKey(
                properties.getTenant().getObjectEnvironment(),
                tenantId,
                knowledgeBaseId,
                documentRef,
                versionNo,
                fileHash,
                fileName
        );
        return saveObjectKey(objectKey, bytes);
    }

    private StoredObject saveObjectKey(String objectKey, byte[] bytes) {
        try {
            Path root = Path.of(properties.getIngestion().getObjectDirectory()).toAbsolutePath().normalize();
            Path target = root.resolve(objectKey).normalize();
            if (!target.startsWith(root)) {
                throw new IllegalArgumentException("Invalid object key");
            }
            Files.createDirectories(target.getParent());
            Files.write(target, bytes);
            return new StoredObject(objectKey, target.toUri().toString(), bytes == null ? 0 : bytes.length);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to save object file", e);
        }
    }

    public byte[] read(String objectKey) {
        try {
            Path file = resolveObjectPath(objectKey);
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read object file: " + objectKey, e);
        }
    }

    public boolean deleteIfExists(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return false;
        }
        try {
            return Files.deleteIfExists(resolveObjectPath(objectKey));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete object file: " + objectKey, e);
        }
    }

    public long countTenantObjects(String environment, Long tenantId) {
        Path prefix = tenantPrefix(environment, tenantId);
        if (!Files.exists(prefix)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(prefix)) {
            return stream.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new IllegalStateException("Failed to count tenant objects: " + tenantId, e);
        }
    }

    public long deleteTenantObjects(String environment, Long tenantId) {
        Path prefix = tenantPrefix(environment, tenantId);
        if (!Files.exists(prefix)) {
            return 0;
        }
        try (Stream<Path> stream = Files.walk(prefix)) {
            java.util.List<Path> paths = stream.sorted(Comparator.reverseOrder()).toList();
            long deleted = 0;
            for (Path path : paths) {
                if (Files.isRegularFile(path) && Files.deleteIfExists(path)) {
                    deleted++;
                } else if (Files.isDirectory(path)) {
                    Files.deleteIfExists(path);
                }
            }
            return deleted;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete tenant objects: " + tenantId, e);
        }
    }

    private Path resolveObjectPath(String objectKey) {
        Path root = Path.of(properties.getIngestion().getObjectDirectory()).toAbsolutePath().normalize();
        Path file = root.resolve(objectKey).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("Invalid object key");
        }
        return file;
    }

    private Path tenantPrefix(String environment, Long tenantId) {
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required");
        }
        Path root = Path.of(properties.getIngestion().getObjectDirectory()).toAbsolutePath().normalize();
        Path prefix = root.resolve(sanitize(StringUtils.hasText(environment) ? environment : "default"))
                .resolve(String.valueOf(tenantId))
                .normalize();
        if (!prefix.startsWith(root)) {
            throw new IllegalArgumentException("Invalid tenant object prefix");
        }
        return prefix;
    }

    private String sanitize(String fileName) {
        String value = StringUtils.hasText(fileName) ? fileName.trim() : "unknown";
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
