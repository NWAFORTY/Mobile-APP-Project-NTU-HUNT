package com.example.treasurehuntapplication;

import android.content.Context;

import androidx.annotation.Nullable;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Single place for {@code users/{documentId}} ids so profile, leaderboard sync, and game state
 * always target the same Firestore document (normalized email).
 */
public final class FirestoreUserDoc {

    private FirestoreUserDoc() {
    }

    /** Document id from a Firebase user, or null if missing. */
    @Nullable
    public static String idForFirebaseUser(@Nullable FirebaseUser u) {
        if (u == null || u.getEmail() == null || u.getEmail().isEmpty()) {
            return null;
        }
        return AuthInputValidator.normalizeEmail(u.getEmail());
    }

    /** Document id from {@link UserSession}, or null if not signed in. */
    @Nullable
    public static String idForSession(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email == null || email.isEmpty()) {
            return null;
        }
        return AuthInputValidator.normalizeEmail(email);
    }

    /**
     * Prefer the signed-in Firebase account’s email, else session email (e.g. offline flows).
     */
    @Nullable
    public static String idForCurrentUser(Context context) {
        String fromAuth = idForFirebaseUser(FirebaseAuth.getInstance().getCurrentUser());
        if (fromAuth != null) {
            return fromAuth;
        }
        return idForSession(context);
    }
}
