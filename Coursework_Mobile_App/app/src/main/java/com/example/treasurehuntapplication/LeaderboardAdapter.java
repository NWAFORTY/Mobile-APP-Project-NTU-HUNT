package com.example.treasurehuntapplication;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Simple list rows for {@link LeaderboardFragment}: rank, display name, score; click → profile dialog.
 */
public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    public interface OnPlayerClickListener {
        void onPlayerClick(LeaderboardModel user);
    }

    private final List<LeaderboardModel> userList;
    @Nullable
    private OnPlayerClickListener onPlayerClickListener;

    public LeaderboardAdapter(List<LeaderboardModel> userList) {
        this.userList = userList;
    }

    public void setOnPlayerClickListener(@Nullable OnPlayerClickListener listener) {
        this.onPlayerClickListener = listener;
    }

    // --- RecyclerView.Adapter ---

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.leaderboard_row_template, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LeaderboardModel user = userList.get(position);

        holder.tvRank.setText(String.valueOf(position + 1));
        holder.tvUsername.setText(user.getDisplayName());
        holder.tvScore.setText(user.getScore() + " pts");

        String sub = user.getProfileLocation();
        if (TextUtils.isEmpty(sub)) {
            holder.playerLocation.setText(R.string.leaderboard_row_campus_hint);
        } else {
            holder.playerLocation.setText(sub);
        }

        holder.playerImage.setImageResource(R.drawable.baseline_person_24);

        holder.itemView.setClickable(true);
        holder.itemView.setFocusable(true);
        holder.itemView.setOnClickListener(v -> {
            if (onPlayerClickListener != null) {
                onPlayerClickListener.onPlayerClick(user);
            }
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    /** Row view holder: rank, name, points. */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvRank;
        TextView tvUsername;
        TextView tvScore;
        ImageView playerImage;
        TextView playerLocation;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRank = itemView.findViewById(R.id.tvRank);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvScore = itemView.findViewById(R.id.tvScore);
            playerImage = itemView.findViewById(R.id.playerImage);
            playerLocation = itemView.findViewById(R.id.playerLocation);
        }
    }
}
