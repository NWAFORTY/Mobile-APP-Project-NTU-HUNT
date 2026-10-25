package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Tracks which treasure the user is currently hunting (after accepting a clue on the treasure screen).
 * Stored per signed-in user so accounts do not share hunt state on the same device.
 */
public final class ActiveTreasureManager {

    // --- Sentinel & preference namespace ---
    private static final int NONE = -1;
    private static final String PREF = "active_treasure";
    private static final String KEY_PREFIX = "active_id_";

    private ActiveTreasureManager() {
    }

    // --- Key per user (email-based) ---

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private static String keyForUser(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email == null || email.isEmpty()) {
            return KEY_PREFIX + "guest";
        }
        return KEY_PREFIX + safeKey(email);
    }

    // --- Active hunt id ---

    public static void setActiveTreasure(Context context, int id) {
        prefs(context).edit().putInt(keyForUser(context), id).apply();
    }

    public static void clearActiveTreasure(Context context) {
        prefs(context).edit().remove(keyForUser(context)).apply();
    }

    public static int getActiveTreasureId(Context context) {
        String k = keyForUser(context);
        if (!prefs(context).contains(k)) {
            return NONE;
        }
        return prefs(context).getInt(k, NONE);
    }

    public static boolean hasActiveTreasure(Context context) {
        return getActiveTreasureId(context) != NONE;
    }

    public static Treasure getActiveTreasure(Context context) {
        int id = getActiveTreasureId(context);
        if (id == NONE) {
            return null;
        }
        return TreasureManager.getById(id);
    }

    public static boolean isActive(Context context, int id) {
        return getActiveTreasureId(context) == id;
    }

    // --- Reset (all users on device) ---

    /** Clears active-hunt state for every account on this device. */
    public static void clearAllActiveTreasureState(Context context) {
        prefs(context).edit().clear().apply();
    }
}
