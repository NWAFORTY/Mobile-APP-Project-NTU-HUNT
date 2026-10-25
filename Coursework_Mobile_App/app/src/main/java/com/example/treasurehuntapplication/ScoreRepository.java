package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import java.util.function.Consumer;

/** Per-user score stored locally; Firestore sync is handled by {@link UserGameStateFirestoreSync}. */
public final class ScoreRepository {

    private static final String PREF = "score_repository";
    private static final String KEY_SCORE = "score_";

    private ScoreRepository() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private static String keyForUser(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email == null || email.isEmpty()) {
            return KEY_SCORE + "guest";
        }
        return KEY_SCORE + safeKey(email);
    }

    public static long getLocalScore(Context context) {
        return prefs(context).getLong(keyForUser(context), 0L);
    }

    public static void addPoints(Context context, int delta) {
        if (delta <= 0) {
            return;
        }
        String k = keyForUser(context);
        long next = prefs(context).getLong(k, 0L) + delta;
        prefs(context).edit().putLong(k, next).apply();
    }

    public static void getDisplayScore(Context context, Consumer<Long> callback) {
        long score = getLocalScore(context);
        new Handler(Looper.getMainLooper()).post(() -> callback.accept(score));
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }
}
