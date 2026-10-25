package com.example.treasurehuntapplication;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/** Ensures a {@code users/{email}} row exists for Firebase-signed-in accounts. */
public final class LeaderboardUserRegistration {

    private LeaderboardUserRegistration() {
    }

    public static void ensureRegistered(Context context) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null || u.getEmail() == null || u.getEmail().isEmpty()) {
            return;
        }
        String docId = FirestoreUserDoc.idForFirebaseUser(u);
        if (docId == null) {
            return;
        }
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Map<String, Object> initial = new HashMap<>();
                        initial.put("uid", u.getUid());
                        initial.put("email", docId);
                        initial.put("username", docId);
                        initial.put("score", ScoreRepository.getLocalScore(context.getApplicationContext()));
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(docId)
                                .set(initial, SetOptions.merge());
                        return;
                    }
                    String existingUid = doc.getString("uid");
                    if (existingUid == null || existingUid.isEmpty()) {
                        Map<String, Object> patch = new HashMap<>();
                        patch.put("uid", u.getUid());
                        FirebaseFirestore.getInstance()
                                .collection("users")
                                .document(docId)
                                .set(patch, SetOptions.merge());
                    }
                });
    }
}
