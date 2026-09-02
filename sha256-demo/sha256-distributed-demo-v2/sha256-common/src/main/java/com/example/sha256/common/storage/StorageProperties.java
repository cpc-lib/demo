package com.example.sha256.common.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "sha256.storage")
public class StorageProperties {
    private String provider = "minio";
    private String endpoint = "http://192.168.1.200:9000";
    /** Browser reachable endpoint used to sign direct-upload URLs. Blank means endpoint. */
    private String publicEndpoint = "";
    private String region = "us-east-1";
    private String accessKey = "sha256minio";
    private String secretKey = "sha256-minio-secret";
    private String bucket = "sha256-files";
    private boolean pathStyleAccess = true;
    private boolean chunkedEncoding = true;
    private boolean autoCreateBucket = true;
    private boolean deleteAfterSuccess = true;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getPublicEndpoint() { return publicEndpoint; }
    public void setPublicEndpoint(String publicEndpoint) { this.publicEndpoint = publicEndpoint; }
    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getAccessKey() { return accessKey; }
    public void setAccessKey(String accessKey) { this.accessKey = accessKey; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public boolean isPathStyleAccess() { return pathStyleAccess; }
    public void setPathStyleAccess(boolean pathStyleAccess) { this.pathStyleAccess = pathStyleAccess; }
    public boolean isChunkedEncoding() { return chunkedEncoding; }
    public void setChunkedEncoding(boolean chunkedEncoding) { this.chunkedEncoding = chunkedEncoding; }
    public boolean isAutoCreateBucket() { return autoCreateBucket; }
    public void setAutoCreateBucket(boolean autoCreateBucket) { this.autoCreateBucket = autoCreateBucket; }
    public boolean isDeleteAfterSuccess() { return deleteAfterSuccess; }
    public void setDeleteAfterSuccess(boolean deleteAfterSuccess) { this.deleteAfterSuccess = deleteAfterSuccess; }
}
