package cc.ivera.ragdemo.service.ragops;

public class IngestionTaskCancelledException extends RuntimeException {

    public IngestionTaskCancelledException(Long taskId) {
        super("Ingestion task cancellation requested: " + taskId);
    }
}
