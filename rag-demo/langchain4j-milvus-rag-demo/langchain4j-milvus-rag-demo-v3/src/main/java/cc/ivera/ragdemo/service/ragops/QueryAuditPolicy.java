package cc.ivera.ragdemo.service.ragops;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

@Component
public class QueryAuditPolicy {

    @Autowired
    public QueryAuditPolicy() {
    }

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String TYPE_QUERY = "QUERY";
    public static final String TYPE_SEARCH = "SEARCH";

    public int hitCount(Collection<?> hits) {
        return hits == null ? 0 : hits.size();
    }

    public boolean knowledgeHit(Collection<?> hits) {
        return hitCount(hits) > 0;
    }

    public long latencyMillis(long startedNanos, long finishedNanos) {
        if (finishedNanos <= startedNanos) {
            return 0L;
        }
        return TimeUnit.NANOSECONDS.toMillis(finishedNanos - startedNanos);
    }

    public String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    public String errorCode(Throwable throwable) {
        return throwable == null ? null : throwable.getClass().getSimpleName();
    }

    public String errorMessage(Throwable throwable, int maxLength) {
        if (throwable == null) {
            return null;
        }
        String message = throwable.getMessage();
        if (message == null || message.isBlank()) {
            message = throwable.toString();
        }
        return truncate(message, maxLength);
    }
}
