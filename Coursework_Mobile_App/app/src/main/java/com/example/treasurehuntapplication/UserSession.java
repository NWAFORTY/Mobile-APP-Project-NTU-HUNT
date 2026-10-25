package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;

/** Persists the signed-in user email for offline flows and for scoping local preferences. */
public final class UserSession {

    private static final String PREF = "user_session";
    private static final String KEY_EMAIL = "email";

    private UserSession() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void signIn(Context context, String email) {
        if (email == null || email.trim().isEmpty()) {
            signOut(context);
            return;
        }
        prefs(context).edit().putString(KEY_EMAIL, email.trim().toLowerCase()).apply();
    }

    public static void signOut(Context context) {
        prefs(context).edit().remove(KEY_EMAIL).apply();
    }

    public static String getEmail(Context context) {
        String e = prefs(context).getString(KEY_EMAIL, null);
        if (e == null || e.isEmpty()) {
            return null;
        }
        return e;
    }

    public static boolean isSignedIn(Context context) {
        return getEmail(context) != null;
    }
}
