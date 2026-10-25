package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Location;

/**
 * Accumulates approximate walk distance from GPS updates (per signed-in user on this device).
 */
public final class WalkDistanceTracker {

    private static final String PREF = "walk_distance";
    private static final String KEY_METERS = "meters_";
    private static final String KEY_LAT = "lat_";
    private static final String KEY_LON = "lon_";
    private static final float MAX_SEGMENT_METERS = 500f;
    private static final String KEY_TOAST_PREFIX = "walk_milestone_toast_";
    private static final int[] WALK_MILESTONES = {10, 100, 500};

    private WalkDistanceTracker() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private static String suffix(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email == null || email.isEmpty()) {
            return "guest";
        }
        return safeKey(email);
    }

    public static void recordLocation(Context context, Location loc) {
        if (loc == null) {
            return;
        }
        SharedPreferences p = prefs(context);
        String s = suffix(context);
        float lastLat = p.getFloat(KEY_LAT + s, Float.NaN);
        float lastLon = p.getFloat(KEY_LON + s, Float.NaN);
        long meters = p.getLong(KEY_METERS + s, 0L);

        if (!Float.isNaN(lastLat) && !Float.isNaN(lastLon)) {
            float[] d = new float[1];
            Location.distanceBetween(lastLat, lastLon, loc.getLatitude(), loc.getLongitude(), d);
            if (d[0] > 0f && d[0] <= MAX_SEGMENT_METERS) {
                meters += (long) d[0];
            }
        }

        p.edit()
                .putLong(KEY_METERS + s, meters)
                .putFloat(KEY_LAT + s, (float) loc.getLatitude())
                .putFloat(KEY_LON + s, (float) loc.getLongitude())
                .apply();
    }

    public static long getTotalMeters(Context context) {
        return prefs(context).getLong(KEY_METERS + suffix(context), 0L);
    }

    /**
     * Records GPS movement, then returns the smallest walk milestone (10 / 100 / 500 m) that was
     * just crossed for the first time — used for a one-shot achievement toast on the map.
     */
    public static int onLocation(Context context, Location loc) {
        recordLocation(context, loc);
        long total = getTotalMeters(context);
        String s = suffix(context);
        SharedPreferences p = prefs(context);
        for (int m : WALK_MILESTONES) {
            String key = KEY_TOAST_PREFIX + m + "_" + s;
            if (total >= m && !p.getBoolean(key, false)) {
                p.edit().putBoolean(key, true).apply();
                return m;
            }
        }
        return 0;
    }

    public static void clearAll(Context context) {
        prefs(context).edit().clear().apply();
    }
}
