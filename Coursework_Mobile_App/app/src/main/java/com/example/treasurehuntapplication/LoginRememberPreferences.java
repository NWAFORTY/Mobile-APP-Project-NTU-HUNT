package com.example.treasurehuntapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.CheckBox;
import android.widget.EditText;

/** Persists “remember me” (username only) for the login form. */
public final class LoginRememberPreferences {

    private static final String PREF = "login_remember";
    private static final String KEY_REMEMBER = "remember";
    private static final String KEY_USERNAME = "username";

    private LoginRememberPreferences() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    public static void loadIntoForm(Activity activity, EditText usernameInput, CheckBox rememberCheck) {
        SharedPreferences p = prefs(activity);
        rememberCheck.setChecked(p.getBoolean(KEY_REMEMBER, false));
        String u = p.getString(KEY_USERNAME, "");
        if (u != null && !u.isEmpty()) {
            usernameInput.setText(u);
        }
    }

    public static void save(Context context, boolean remember, String username) {
        SharedPreferences.Editor e = prefs(context).edit();
        if (remember && username != null && !username.trim().isEmpty()) {
            e.putBoolean(KEY_REMEMBER, true);
            e.putString(KEY_USERNAME, username.trim());
        } else {
            e.putBoolean(KEY_REMEMBER, false);
            e.remove(KEY_USERNAME);
        }
        e.apply();
    }

    public static void clear(Context context) {
        prefs(context).edit().clear().apply();
    }
}
