package com.example.vocab.security;

public final class CurrentUser {
    private CurrentUser() {}
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    public static void set(Long userId) { USER_ID.set(userId); }
    public static Long id() { return USER_ID.get(); }
    public static Long requiredId() {
        Long id = USER_ID.get();
        if (id == null) throw new IllegalArgumentException("login required");
        return id;
    }
    public static void clear() { USER_ID.remove(); }
}
