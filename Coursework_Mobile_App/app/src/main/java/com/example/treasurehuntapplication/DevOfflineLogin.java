package com.example.treasurehuntapplication;

/**
 * Optional extra offline credentials for development. No pairs are enabled by default;
 * extend {@link #matches(String, String)} locally if you need demo accounts beyond {@code a}/{@code a}.
 */
public final class DevOfflineLogin {

    public static final String OFFLINE_SESSION_EMAIL = "offline.dev@ntu.local";

    private DevOfflineLogin() {
    }

    public static boolean matches(String rawId, String password) {
        return false;
    }
}
