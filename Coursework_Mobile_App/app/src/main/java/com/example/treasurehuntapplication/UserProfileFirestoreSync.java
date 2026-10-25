package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/** Syncs display name with {@code users/{email}.username} and hydrates from Firestore once per account. */
public final class UserProfileFirestoreSync {

    private static final String PREF = "profile_fs_hydration";
    private static final String KEY_HYDRATED = "hydrated_";

    private UserProfileFirestoreSync() {
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    public static void clearAccountHydrationMarker(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email != null && !email.isEmpty()) {
            prefs(context).edit().remove(KEY_HYDRATED + safeKey(email)).apply();
        }
    }

    public static void hydrateDisplayNameForCurrentAccount(Context context) {
        Context app = context.getApplicationContext();
        String docId = FirestoreUserDoc.idForCurrentUser(app);
        if (docId == null || docId.isEmpty()) {
            return;
        }
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null || u.getEmail().isEmpty()) {
            return;
        }
        String key = KEY_HYDRATED + safeKey(docId);
        if (prefs(app).getBoolean(key, false)) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String un = doc.getString("username");
                        String local = ProfilePreferences.getDisplayName(app);
                        if (un != null && !un.trim().isEmpty()
                                && (local == null || local.trim().isEmpty())) {
                            ProfilePreferences.setDisplayName(app, un.trim());
                        }
                    }
                    prefs(app).edit().putBoolean(key, true).apply();
                })
                .addOnFailureListener(e -> prefs(app).edit().putBoolean(key, true).apply());
    }

    public static void pushDisplayName(Context context, String displayName) {
        Context app = context.getApplicationContext();
        String docId = FirestoreUserDoc.idForCurrentUser(app);
        if (docId == null || docId.isEmpty()) {
            return;
        }
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            Toast.makeText(app, R.string.profile_sync_needs_firebase_auth, Toast.LENGTH_LONG).show();
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("uid", u.getUid());
        data.put("username", displayName != null ? displayName : "");
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .set(data, SetOptions.merge())
                .addOnSuccessListener(v ->
                        Toast.makeText(app, R.string.profile_sync_leaderboard_ok, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(app, R.string.profile_sync_leaderboard_failed, Toast.LENGTH_LONG).show());
    }

    /** Pushes campus/location line for leaderboard subtitle ({@code profileLocation} on {@code users/{email}}). */
    public static void pushProfileLocation(Context context, String location) {
        Context app = context.getApplicationContext();
        String docId = FirestoreUserDoc.idForCurrentUser(app);
        if (docId == null || docId.isEmpty()) {
            return;
        }
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("uid", u.getUid());
        data.put("profileLocation", location != null ? location : "");
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .set(data, SetOptions.merge());
    }
}
