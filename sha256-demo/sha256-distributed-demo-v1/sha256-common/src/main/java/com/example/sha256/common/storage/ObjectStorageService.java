package com.example.sha256.common.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface ObjectStorageService {
    void putObject(Path localFile, String storageKey, String contentType);
    InputStream getObject(String storageKey);
    void deleteObject(String storageKey);
    String bucket();
    String provider();
}
