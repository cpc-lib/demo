package cc.ivera.migration.domain;

public interface MigrationStatus {
    String PLANNED = "PLANNED";
    String RUNNING = "RUNNING";
    String SUCCESS = "SUCCESS";
    String FAILED = "FAILED";
    String PARTIAL_FAILED = "PARTIAL_FAILED";
    String VERIFYING = "VERIFYING";
    String VERIFIED = "VERIFIED";
    String SWITCHED = "SWITCHED";
    String RETRYING = "RETRYING";
}
