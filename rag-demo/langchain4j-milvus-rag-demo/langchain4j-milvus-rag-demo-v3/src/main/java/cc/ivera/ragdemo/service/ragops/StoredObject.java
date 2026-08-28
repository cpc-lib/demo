package cc.ivera.ragdemo.service.ragops;

public record StoredObject(
        String objectKey,
        String uri,
        long size
) {
}
