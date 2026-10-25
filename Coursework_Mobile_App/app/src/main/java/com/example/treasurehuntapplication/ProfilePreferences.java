package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;

/** Local profile text fields, scoped by {@link UserSession} email. */
public final class ProfilePreferences {

    private static final String PREF = "profile_fields";
    private static final String KEY_DISPLAY = "display_";
    private static final String KEY_LOCATION = "location_";
    private static final String KEY_COURSE = "course_";
    private static final String KEY_PRONOUNS = "pronouns_";

    private ProfilePreferences() {
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

    public static String getDisplayName(Context context) {
        return prefs(context).getString(KEY_DISPLAY + suffix(context), "");
    }

    public static void setDisplayName(Context context, String value) {
        prefs(context).edit().putString(KEY_DISPLAY + suffix(context), value != null ? value : "").apply();
    }

    public static String getLocation(Context context) {
        return prefs(context).getString(KEY_LOCATION + suffix(context), "");
    }

    public static void setLocation(Context context, String value) {
        prefs(context).edit().putString(KEY_LOCATION + suffix(context), value != null ? value : "").apply();
    }

    public static String getCourse(Context context) {
        return prefs(context).getString(KEY_COURSE + suffix(context), "");
    }

    public static void setCourse(Context context, String value) {
        prefs(context).edit().putString(KEY_COURSE + suffix(context), value != null ? value : "").apply();
    }

    public static String getPronouns(Context context) {
        return prefs(context).getString(KEY_PRONOUNS + suffix(context), "");
    }

    public static void setPronouns(Context context, String value) {
        prefs(context).edit().putString(KEY_PRONOUNS + suffix(context), value != null ? value : "").apply();
    }
}
