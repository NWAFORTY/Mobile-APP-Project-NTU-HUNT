package com.example.treasurehuntapplication;

import androidx.annotation.NonNull;

import com.google.firebase.firestore.DocumentSnapshot;

/**
 * Firestore user fields used on the leaderboard.
 * Documents use {@code username} (display) and {@code score}; {@code email} is a fallback for older docs.
 */
public class LeaderboardModel {

    // --- Row fields (from Firestore or local demo) ---
    private String username;
    private String email;
    private long score;
    /** Optional subtitle under the display name (e.g. campus line from profile settings). */
    private String profileLocation;
    /** Firestore document id (used to load full profile); may differ from email in edge cases. */
    private String firestoreDocumentId;
    /** Firebase Auth uid when present — matches the signed-in user even if email fields differ. */
    private String firebaseUid;

    public LeaderboardModel() {
    }

    public LeaderboardModel(String username, long score) {
        this.username = username;
        this.score = score;
    }

    // --- Firestore mapping ---

    /**
     * Reads only leaderboard fields. Avoids {@link DocumentSnapshot#toObject(Class)} so extra
     * document keys (e.g. legacy {@code password}, nested profile maps) do not spam the mapper.
     */
    @NonNull
    public static LeaderboardModel fromDocument(@NonNull DocumentSnapshot document) {
        LeaderboardModel user = new LeaderboardModel();
        if (document.contains("score")) {
            Long s = document.getLong("score");
            user.setScore(s != null ? s : 0L);
        }
        String docEmail = document.getString("email");
        if (docEmail == null || docEmail.isEmpty()) {
            docEmail = document.getId();
        }
        user.setEmail(docEmail);
        String u = document.getString("username");
        user.setUsername(u != null && !u.isEmpty() ? u : docEmail);
        String loc = document.getString("profileLocation");
        user.setProfileLocation(loc != null ? loc : "");
        String uid = document.getString("uid");
        user.setFirebaseUid(uid != null ? uid : "");
        user.setFirestoreDocumentId(document.getId());
        return user;
    }

    // --- Accessors ---

    public String getFirestoreDocumentId() {
        if (firestoreDocumentId != null && !firestoreDocumentId.isEmpty()) {
            return firestoreDocumentId;
        }
        if (email != null && !email.isEmpty()) {
            return email.trim();
        }
        return "";
    }

    public void setFirestoreDocumentId(String firestoreDocumentId) {
        this.firestoreDocumentId = firestoreDocumentId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public long getScore() {
        return score;
    }

    public void setScore(long score) {
        this.score = score;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getProfileLocation() {
        return profileLocation != null ? profileLocation : "";
    }

    public void setProfileLocation(String profileLocation) {
        this.profileLocation = profileLocation != null ? profileLocation : "";
    }

    public String getFirebaseUid() {
        return firebaseUid != null ? firebaseUid : "";
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid != null ? firebaseUid : "";
    }

    /** Prefer username; fall back to email for legacy documents that only stored email. */
    public String getDisplayName() {
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        if (email != null && !email.trim().isEmpty()) {
            return email;
        }
        return "Player";
    }
}
