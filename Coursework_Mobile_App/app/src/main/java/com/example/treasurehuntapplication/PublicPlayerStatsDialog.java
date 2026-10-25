package com.example.treasurehuntapplication;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;
import java.util.Locale;

import de.hdodenhof.circleimageview.CircleImageView;

/** Loads another player’s public Firestore profile for the leaderboard row tap. */
public final class PublicPlayerStatsDialog {

    private PublicPlayerStatsDialog() {
    }

    public static void show(Fragment parent, LeaderboardModel user) {
        if (!parent.isAdded()) {
            return;
        }
        String docId = user.getFirestoreDocumentId();
        if (docId == null || docId.isEmpty()) {
            Toast.makeText(parent.requireContext(), R.string.public_player_profile_unavailable,
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog loading = NtuMaterialDialogs.builder(parent.requireContext())
                .setTitle(R.string.public_player_dialog_title)
                .setMessage(R.string.public_player_loading)
                .setCancelable(true)
                .create();
        loading.show();

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(docId)
                .get()
                .addOnCompleteListener(task -> {
                    if (!parent.isAdded()) {
                        return;
                    }
                    loading.dismiss();
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        Toast.makeText(parent.requireContext(), R.string.public_player_load_failed,
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    DocumentSnapshot doc = task.getResult();
                    String name = doc.getString("username");
                    if (name == null || name.isEmpty()) {
                        name = user.getDisplayName();
                    }
                    Long scoreObj = doc.getLong("score");
                    long score = scoreObj != null ? scoreObj : 0L;
                    int nFound = parseFoundCount(doc);

                    String achievements = parent.getString(R.string.public_player_achievements_heading)
                            + "\n· " + parent.getString(R.string.achievement_treasure_1)
                            + "\n· " + parent.getString(R.string.achievement_walk_100m);

                    String treasuresHeading = parent.getString(R.string.public_player_treasures_heading);
                    String body = String.format(Locale.getDefault(),
                            "%s\n\n%s: %d\n\n%s\n\n%s: %d",
                            name,
                            parent.getString(R.string.leaderboard_title),
                            score,
                            achievements,
                            treasuresHeading,
                            nFound);

                    View content = LayoutInflater.from(parent.requireContext())
                            .inflate(R.layout.dialog_public_player, null);
                    CircleImageView avatar = content.findViewById(R.id.publicPlayerAvatar);
                    ((TextView) content.findViewById(R.id.publicPlayerBody)).setText(body);
                    avatar.setImageResource(R.drawable.baseline_person_24);
                    AlertDialog dialog = NtuMaterialDialogs.builder(parent.requireContext())
                            .setView(content)
                            .create();
                    NtuMaterialDialogs.applyCustomContentWindow(dialog, 0.65f);
                    content.findViewById(R.id.publicPlayerOk).setOnClickListener(v -> dialog.dismiss());
                    dialog.show();
                });
    }

    private static int parseFoundCount(DocumentSnapshot doc) {
        Object raw = doc.get("foundTreasureIds");
        if (!(raw instanceof List<?>)) {
            return 0;
        }
        List<?> list = (List<?>) raw;
        return list.size();
    }
}
