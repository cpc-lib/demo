package cc.ivera.audit.provider;

public interface ChangeLogSnapshotProvider {

    boolean supports(Class<?> entityClass);

    Object loadById(Object id);
}
