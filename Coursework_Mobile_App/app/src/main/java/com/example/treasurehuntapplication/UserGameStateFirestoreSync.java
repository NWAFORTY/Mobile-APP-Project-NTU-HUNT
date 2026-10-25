package com.example.treasurehuntapplication;

import android.content.Context;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Pushes score, walk distance, and found-treasure ids to {@code users/{email}}. */
public final class UserGameStateFirestoreSync {

    private UserGameStateFirestoreSync() {
    }

    private static String documentId(Context context) {
        return FirestoreUserDoc.idForCurrentUser(context.getApplicationContext());
    }

    public static void pushGameStateToFirestore(Context context) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            return;
        }
        String docId = documentId(context);
        if (docId == null || docId.isEmpty()) {
            return;
        }
        List<Long> found = TreasureManager.getFoundTreasureIdsForSync(context);
        Map<String, Object> data = new HashMap<>();
        data.put("uid", u.getUid());
        data.put("score", ScoreRepository.getLocalScore(context));
        data.put("totalWalkMeters", WalkDistanceTracker.getTotalMeters(context));
        data.put("foundTreasureIds", found);

        String campusLine = ProfilePreferences.getLocation(context.getApplicationContext());
        if (campusLine != null && !campusLine.trim().isEmpty()) {
            data.put("profileLocation", campusLine.trim());
        }

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .set(data, SetOptions.merge());
    }

    public static void wipeRemoteGameFieldsForCurrentUser(Context context) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u == null) {
            return;
        }
        String docId = FirestoreUserDoc.idForFirebaseUser(u);
        if (docId == null || docId.isEmpty()) {
            return;
        }
        Map<String, Object> data = new HashMap<>();
        data.put("score", 0L);
        data.put("totalWalkMeters", 0L);
        data.put("foundTreasureIds", java.util.Collections.emptyList());

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .set(data, SetOptions.merge());
    }
}
