package com.example.treasurehuntapplication;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/** One-time “how to play” dialog per signed-in account (Firebase UID when available). */
public final class TutorialPopup {

    private static final String PREF = "tutorial_popup";
    private static final String KEY_PREFIX = "seen_";

    private TutorialPopup() {
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    /** Stable id: Firebase {@code uid}, else normalized email, else {@code guest}. */
    private static String tutorialAccountKey(Context context) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null) {
            String uid = u.getUid();
            if (uid != null && !uid.isEmpty()) {
                return uid;
            }
        }
        String email = UserSession.getEmail(context);
        if (email != null && !email.isEmpty()) {
            return safeKey(email);
        }
        return "guest";
    }

    public static void showIfFirstTime(Activity activity) {
        String key = KEY_PREFIX + tutorialAccountKey(activity);
        if (prefs(activity).getBoolean(key, false)) {
            return;
        }

        View content = LayoutInflater.from(activity).inflate(R.layout.dialog_tutorial, null);
        AlertDialog dialog = NtuMaterialDialogs.builder(activity)
                .setView(content)
                .setCancelable(false)
                .create();
        NtuMaterialDialogs.applyCustomContentWindow(dialog, 0.65f);

        content.findViewById(R.id.tutorialOk).setOnClickListener(v -> {
            prefs(activity).edit().putBoolean(key, true).apply();
            dialog.dismiss();
        });

        dialog.show();
    }
}
