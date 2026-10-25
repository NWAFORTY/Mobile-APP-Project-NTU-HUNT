package com.example.treasurehuntapplication;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;

import com.example.treasurehuntapplication.Treasure.Rarity;

/**
 * Browse treasures by rarity: tap a tile for clue + hint, accept to set {@link ActiveTreasureManager}
 * and switch to the Map tab in {@link navigation}.
 */
public class TreasureFragment extends Fragment {

    public TreasureFragment() {
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_treasure, container, false);
        populateTreasures(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            populateTreasures(getView());
        }
    }

    /** Rebuilds horizontal lists per rarity and updates section headers with counts. */
    private void populateTreasures(View view) {
        LinearLayout commonContainer = view.findViewById(R.id.commonContainer);
        LinearLayout rareContainer = view.findViewById(R.id.rareContainer);
        LinearLayout legendaryContainer = view.findViewById(R.id.legendaryContainer);

        TextView commonHeader = view.findViewById(R.id.commonCount);
        TextView rareHeader = view.findViewById(R.id.rareCount);
        TextView legendaryHeader = view.findViewById(R.id.legendaryCount);

        commonHeader.setText(String.format("Common — %d treasures", TreasureManager.countByRarity(Rarity.COMMON)));
        rareHeader.setText(String.format("Rare — %d treasures", TreasureManager.countByRarity(Rarity.RARE)));
        legendaryHeader.setText(String.format("Legendary — %d treasure", TreasureManager.countByRarity(Rarity.LEGENDARY)));

        commonContainer.removeAllViews();
        rareContainer.removeAllViews();
        legendaryContainer.removeAllViews();

        for (Treasure t : TreasureManager.getByRarity(Rarity.COMMON)) {
            addTreasureTile(commonContainer, t);
        }
        for (Treasure t : TreasureManager.getByRarity(Rarity.RARE)) {
            addTreasureTile(rareContainer, t);
        }
        for (Treasure t : TreasureManager.getByRarity(Rarity.LEGENDARY)) {
            addTreasureTile(legendaryContainer, t);
        }
    }

    /** One scroll tile: coloured card, tap opens popup if not yet collected. */
    private void addTreasureTile(LinearLayout container, Treasure treasure) {
        MaterialCardView wrapper = new MaterialCardView(requireContext());
        LinearLayout.LayoutParams wrapParams =
                new LinearLayout.LayoutParams(500, 500);
        wrapParams.setMargins(0, 0, 20, 0);
        wrapper.setLayoutParams(wrapParams);
        float density = getResources().getDisplayMetrics().density;
        int cornerPx = Math.round(12 * density);
        int strokePx = Math.max(1, Math.round(1 * density));
        wrapper.setRadius(cornerPx);
        wrapper.setCardElevation(0f);
        wrapper.setStrokeWidth(strokePx);
        wrapper.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.ntu_leaderboard_row_stroke)));
        wrapper.setCardBackgroundColor(tileColor(treasure.getRarity()));
        wrapper.setPreventCornerOverlap(false);
        wrapper.setUseCompatPadding(false);

        ImageView img = new ImageView(requireContext());
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        img.setLayoutParams(imgParams);
        boolean found = TreasureManager.isFound(requireContext(), treasure.getId());
        // Do not use setEnabled(false) when collected — that blocks long-press for "reset for testing".
        img.setAlpha(found ? 0.88f : 1f);

        if (!found) {
            img.setOnClickListener(v -> showTreasurePopup(treasure));
        }

        View.OnLongClickListener resetCollectedListener = v -> {
            if (!TreasureManager.isFound(requireContext(), treasure.getId())) {
                return false;
            }
            NtuMaterialDialogs.builder(requireContext())
                    .setTitle(R.string.treasure_reset_collected_title)
                    .setMessage(R.string.treasure_reset_collected_message)
                    .setPositiveButton(R.string.treasure_reset_collected_confirm, (d, w) -> {
                        TreasureManager.unmarkFound(requireContext(), treasure.getId());
                        View root = getView();
                        if (root != null) {
                            populateTreasures(root);
                        }
                    })
                    .setNegativeButton(R.string.settings_cancel, null)
                    .show();
            return true;
        };
        img.setLongClickable(true);
        img.setHapticFeedbackEnabled(true);
        img.setOnLongClickListener(resetCollectedListener);

        wrapper.addView(img);

        if (found) {
            TextView badge = new TextView(requireContext());
            badge.setText(R.string.treasure_collected_badge);
            badge.setTextColor(Color.WHITE);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setTextSize(14f);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundColor(0xCC000000);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.gravity = Gravity.BOTTOM;
            badge.setPadding(8, 10, 8, 10);
            badge.setLongClickable(true);
            badge.setHapticFeedbackEnabled(true);
            badge.setOnLongClickListener(resetCollectedListener);
            wrapper.addView(badge, badgeParams);
        }

        container.addView(wrapper);
    }

    /** Background tint by rarity (matches Stats screen palette). */
    private static int tileColor(Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return Color.rgb(76, 175, 80);
            case RARE:
                return Color.rgb(33, 150, 243);
            case LEGENDARY:
                return Color.rgb(255, 193, 7);
            default:
                return Color.GRAY;
        }
    }

    /** Clue dialog; Accept stores active treasure id and navigates to map. */
    private void showTreasurePopup(Treasure treasure) {

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.item_hint, null);

        TextView tvTitle = dialogView.findViewById(R.id.tvTitle);
        TextView tvHint = dialogView.findViewById(R.id.tvHint);
        Button btnAccept = dialogView.findViewById(R.id.btnAccept);

        tvTitle.setText(treasure.getTitle());
        tvHint.setText(treasure.getClue() + "\n\nHint: " + treasure.getHint());

        AlertDialog dialog = NtuMaterialDialogs.builder(requireContext())
                .setView(dialogView)
                .create();
        NtuMaterialDialogs.applyCustomContentWindow(dialog, 0.7f);
        dialog.show();

        btnAccept.setOnClickListener(v -> {
            ActiveTreasureManager.setActiveTreasure(requireContext(), treasure.getId());
            dialog.dismiss();
            if (getActivity() instanceof navigation) {
                ((navigation) getActivity()).showMapTab();
            }
        });
    }
}
