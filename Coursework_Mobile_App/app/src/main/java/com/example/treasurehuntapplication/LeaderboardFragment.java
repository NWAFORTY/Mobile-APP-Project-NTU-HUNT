package com.example.treasurehuntapplication;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Live Firestore leaderboard: listens to {@code users} collection, sorts by score, shows top N,
 * empty state, and opens {@link PublicPlayerStatsDialog} on row tap.
 */
public class LeaderboardFragment extends Fragment {

    private static final String TAG = "LeaderboardFragment";
    private static final int MAX_FETCH = 200;
    private static final int TOP_N = 50;

    private RecyclerView recyclerView;
    private TextView emptyView;
    private LeaderboardAdapter adapter;
    private List<LeaderboardModel> userList;
    private FirebaseFirestore db;
    @Nullable
    private ListenerRegistration leaderboardRegistration;

    public LeaderboardFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_leaderboard, container, false);

        // --- RecyclerView + adapter ---
        userList = new ArrayList<>();
        adapter = new LeaderboardAdapter(userList);

        recyclerView = view.findViewById(R.id.leaderboardRecycler);
        emptyView = view.findViewById(R.id.leaderboardEmpty);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        recyclerView.setAdapter(adapter);
        adapter.setOnPlayerClickListener(user -> PublicPlayerStatsDialog.show(LeaderboardFragment.this, user));

        db = FirebaseFirestore.getInstance();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // --- Subscribe to Firestore (active while fragment started) ---
        attachLeaderboardListener();
    }

    @Override
    public void onStop() {
        detachLeaderboardListener();
        super.onStop();
    }

    /** Removes snapshot listener to avoid leaks and wasted reads. */
    private void detachLeaderboardListener() {
        if (leaderboardRegistration != null) {
            leaderboardRegistration.remove();
            leaderboardRegistration = null;
        }
    }

    /** Attaches listener to {@code users} with limit; handles errors and drives {@link #applyLeaderboardDocuments}. */
    private void attachLeaderboardListener() {
        detachLeaderboardListener();

        leaderboardRegistration = db.collection("users")
                .limit(MAX_FETCH)
                .addSnapshotListener((QuerySnapshot snapshot, com.google.firebase.firestore.FirebaseFirestoreException error) -> {
                    if (!isAdded()) {
                        return;
                    }
                    if (error != null) {
                        Log.e(TAG, "Leaderboard listener failed", error);
                        if (getContext() != null) {
                            String msg = error.getMessage() != null ? error.getMessage() : "Unknown error";
                            Toast.makeText(getContext(), getString(R.string.leaderboard_error, msg), Toast.LENGTH_LONG).show();
                        }
                        updateEmptyState();
                        return;
                    }
                    if (snapshot == null) {
                        return;
                    }
                    applyLeaderboardDocuments(snapshot);
                });
    }

    /** Parses documents, sorts descending by score, trims to TOP_N. */
    private void applyLeaderboardDocuments(QuerySnapshot snapshot) {
        userList.clear();
        for (DocumentSnapshot document : snapshot.getDocuments()) {
            userList.add(LeaderboardModel.fromDocument(document));
        }

        Collections.sort(userList, (a, b) -> Long.compare(b.getScore(), a.getScore()));
        if (userList.size() > TOP_N) {
            List<LeaderboardModel> top = new ArrayList<>(userList.subList(0, TOP_N));
            userList.clear();
            userList.addAll(top);
        }

        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    /** Toggles placeholder vs list when there are zero rows. */
    private void updateEmptyState() {
        if (emptyView == null || recyclerView == null) {
            return;
        }
        boolean empty = userList.isEmpty();
        emptyView.setVisibility(empty ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(empty ? View.GONE : View.VISIBLE);
    }
}
