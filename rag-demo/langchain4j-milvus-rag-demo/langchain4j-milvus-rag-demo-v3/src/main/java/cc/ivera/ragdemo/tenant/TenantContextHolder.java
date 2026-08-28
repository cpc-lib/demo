package cc.ivera.ragdemo.tenant;

import cc.ivera.ragdemo.exception.MissingTenantContextException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public final class TenantContextHolder {

    private static final ThreadLocal<TenantContext> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> BYPASS_FLAG = new ThreadLocal<>();

    private TenantContextHolder() {
    }

    public static void set(TenantContext context) {
        if (context == null) {
            clear();
            return;
        }
        HOLDER.set(context);
    }

    public static Optional<TenantContext> current() {
        return Optional.ofNullable(HOLDER.get());
    }

    public static TenantContext require() {
        TenantContext context = HOLDER.get();
        if (context == null) {
            throw new MissingTenantContextException("Tenant context is required");
        }
        return context;
    }

    public static Optional<Long> currentTenantId() {
        return current().map(TenantContext::tenantId);
    }

    public static Long requireTenantId() {
        Long tenantId = require().tenantId();
        if (tenantId == null) {
            throw new MissingTenantContextException("Tenant id is required");
        }
        return tenantId;
    }

    public static UserContext requireUser() {
        UserContext user = require().user();
        if (user == null) {
            throw new MissingTenantContextException("User context is required");
        }
        return user;
    }

    public static void clear() {
        HOLDER.remove();
    }

    public static void runWith(TenantContext context, Runnable runnable) {
        TenantContext previous = HOLDER.get();
        set(context);
        try {
            runnable.run();
        } finally {
            restore(previous);
        }
    }

    public static <T> T callWith(TenantContext context, Supplier<T> supplier) {
        TenantContext previous = HOLDER.get();
        set(context);
        try {
            return supplier.get();
        } finally {
            restore(previous);
        }
    }

    public static TenantContext systemContext(Long tenantId, String reason) {
        UserContext user = new UserContext("system", "System", List.of("SYSTEM"), List.of(), List.of(), List.of());
        return new TenantContext(
                tenantId,
                null,
                user,
                "sys-" + UUID.randomUUID().toString().replace("-", ""),
                "system",
                true,
                false,
                tenantId,
                reason,
                Instant.now()
        );
    }

    // ==================== Tenant filter bypass ====================

    /**
     * Returns {@code true} when the current thread is executing a cross-tenant
     * administrative operation that should bypass the MyBatis-Plus tenant line
     * interceptor. The {@link cc.ivera.ragdemo.config.TenantLineInterceptorConfig}
     * checks this flag in {@code ignoreTable} and returns {@code true} for all
     * tables, effectively disabling tenant filtering for the duration of the
     * bypass scope.
     */
    public static boolean isBypass() {
        return Boolean.TRUE.equals(BYPASS_FLAG.get());
    }

    /**
     * Runs a supplier with tenant filtering temporarily disabled. This is
     * intended for platform-level administrative operations that legitimately
     * need to read or modify data across all tenants (e.g. listing all users
     * for tenant management). The previous bypass state is always restored in
     * the {@code finally} block, so nested calls are safe.
     */
    public static <T> T callWithBypass(Supplier<T> supplier) {
        Boolean previous = BYPASS_FLAG.get();
        BYPASS_FLAG.set(true);
        try {
            return supplier.get();
        } finally {
            restoreBypass(previous);
        }
    }

    /**
     * Runs a runnable with tenant filtering temporarily disabled. See
     * {@link #callWithBypass(Supplier)} for semantics.
     */
    public static void runWithBypass(Runnable runnable) {
        Boolean previous = BYPASS_FLAG.get();
        BYPASS_FLAG.set(true);
        try {
            runnable.run();
        } finally {
            restoreBypass(previous);
        }
    }

    private static void restoreBypass(Boolean previous) {
        if (previous == null) {
            BYPASS_FLAG.remove();
        } else {
            BYPASS_FLAG.set(previous);
        }
    }

    private static void restore(TenantContext previous) {
        if (previous == null) {
            clear();
        } else {
            HOLDER.set(previous);
        }
    }
}
