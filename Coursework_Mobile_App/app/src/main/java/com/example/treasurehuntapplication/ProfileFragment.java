package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Gravity;
import android.widget.FrameLayout;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;

import com.example.treasurehuntapplication.Treasure.Rarity;

public class ProfileFragment extends Fragment {

    private static final int BADGE_TARGET_PX = 200;
    private static final LruCache<Integer, Bitmap> BADGE_BITMAP_CACHE = new LruCache<>(
            Math.max(8, (int) (Runtime.getRuntime().maxMemory() / 1024L / 12L))
    );

    private TextView commonCount;
    private TextView rareCount;
    private TextView legendaryCount;
    private TextView totalPointsText;
    private TextView profileName;
    private TextView profileLocation;
    private TextView profileCourse;
    private TextView profilePronouns;
    private ImageView profileImage;
    private GridLayout badgeGrid;
    private GridLayout treasureGrid;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // --- Grids & text views ---
        badgeGrid = view.findViewById(R.id.badgeGrid);
        treasureGrid = view.findViewById(R.id.treasureGrid);
        commonCount = view.findViewById(R.id.commonCount);
        rareCount = view.findViewById(R.id.rareCount);
        legendaryCount = view.findViewById(R.id.legendaryCount);
        totalPointsText = view.findViewById(R.id.totalPointsText);
        profileName = view.findViewById(R.id.profileName);
        profileLocation = view.findViewById(R.id.profileLocation);
        profileCourse = view.findViewById(R.id.profileCourse);
        profilePronouns = view.findViewById(R.id.profilePronouns);
        profileImage = view.findViewById(R.id.profileImage);

        // --- Navigation: settings fragment ---
        view.findViewById(R.id.btnOpenSettings).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SettingsFragment())
                        .addToBackStack(null)
                        .commit());

        // --- Sign out → Login ---
        view.findViewById(R.id.btnSignOut).setOnClickListener(v -> confirmSignOut());

        refreshStats(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        UserGameStateFirestoreSync.pushGameStateToFirestore(requireContext());
        if (getView() != null) {
            refreshStats(getView());
        }
    }

    /** Confirms before clearing Firebase + local session state. */
    private void confirmSignOut() {
        NtuMaterialDialogs.builder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setPositiveButton(R.string.logout_confirm, (d, w) -> performSignOut())
                .setNegativeButton(R.string.settings_cancel, null)
                .show();
    }

    /** Full logout: Auth, prefs, remember-me, return to {@link Login}. */
    private void performSignOut() {
        FirebaseAuth.getInstance().signOut();
        ActiveTreasureManager.clearActiveTreasure(requireContext());
        UserProfileFirestoreSync.clearAccountHydrationMarker(requireContext());
        UserSession.signOut(requireContext());
        LoginRememberPreferences.clear(requireContext());
        Intent i = new Intent(requireActivity(), Login.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(i);
        requireActivity().finish();
    }

    /** Binds profile fields, rarity counts, points, badges, and treasure mini-cells. */
    private void refreshStats(View view) {
        String email = UserSession.getEmail(requireContext());
        String displayName = ProfilePreferences.getDisplayName(requireContext());
        if (displayName != null && !displayName.isEmpty()) {
            profileName.setText(displayName);
        } else {
            profileName.setText(email != null ? email : getString(R.string.stats_guest_name));
        }

        String loc = ProfilePreferences.getLocation(requireContext());
        profileLocation.setText(loc == null || loc.isEmpty()
                ? getString(R.string.stats_profile_campus_line)
                : loc);

        String course = ProfilePreferences.getCourse(requireContext());
        if (course != null && !course.isEmpty()) {
            profileCourse.setVisibility(View.VISIBLE);
            profileCourse.setText(course);
        } else {
            profileCourse.setVisibility(View.GONE);
        }

        String pronouns = ProfilePreferences.getPronouns(requireContext());
        if (pronouns != null && !pronouns.isEmpty()) {
            profilePronouns.setVisibility(View.VISIBLE);
            profilePronouns.setText(pronouns);
        } else {
            profilePronouns.setVisibility(View.GONE);
        }

        profileImage.setImageResource(R.drawable.baseline_person_24);
        ImageViewCompat.setImageTintList(profileImage,
                AppCompatResources.getColorStateList(requireContext(), R.color.ntu_support_text_muted));

        int foundCommon = countFound(requireContext(), Rarity.COMMON);
        int foundRare = countFound(requireContext(), Rarity.RARE);
        int foundLeg = countFound(requireContext(), Rarity.LEGENDARY);
        int totalCommon = TreasureManager.countByRarity(Rarity.COMMON);
        int totalRare = TreasureManager.countByRarity(Rarity.RARE);
        int totalLeg = TreasureManager.countByRarity(Rarity.LEGENDARY);

        commonCount.setText(getString(R.string.stats_rarity_count, foundCommon, totalCommon));
        rareCount.setText(getString(R.string.stats_rarity_count, foundRare, totalRare));
        legendaryCount.setText(getString(R.string.stats_rarity_count, foundLeg, totalLeg));

        ScoreRepository.getDisplayScore(requireContext(), score ->
                totalPointsText.setText(getString(R.string.stats_total_points, String.valueOf(score))));

        badgeGrid.removeAllViews();
        long walked = WalkDistanceTracker.getTotalMeters(requireContext());
        int foundTotal = TreasureManager.getFoundCount(requireContext());
        for (WalkAchievements.Badge b : WalkAchievements.getWalkBadges()) {
            addWalkAchievementBadge(badgeGrid, b, walked);
        }
        for (TreasureAchievements.Badge b : TreasureAchievements.getTreasureBadges()) {
            addTreasureAchievementBadge(badgeGrid, b, foundTotal);
        }

        treasureGrid.removeAllViews();
        for (Treasure t : TreasureManager.getAll()) {
            addTreasureCell(treasureGrid, t);
        }
    }

    /** How many treasures of this rarity are marked found for current user. */
    private static int countFound(Context context, Rarity rarity) {
        int c = 0;
        for (Treasure t : TreasureManager.getByRarity(rarity)) {
            if (TreasureManager.isFound(context, t.getId())) {
                c++;
            }
        }
        return c;
    }

    private void addWalkAchievementBadge(GridLayout grid, WalkAchievements.Badge b, long walkedMeters) {
        boolean unlocked = walkedMeters >= b.requiredMeters;
        HexagonImageView badge = createHexBadgeView(grid, b.drawableRes, unlocked, getString(b.labelRes));
        badge.setOnClickListener(v -> showAchievementDetail(
                b.labelRes, b.detailRes, unlocked, true, walkedMeters, 0, b.requiredMeters));
        grid.addView(badge);
    }

    private void addTreasureAchievementBadge(GridLayout grid, TreasureAchievements.Badge b, int foundTotal) {
        boolean unlocked = foundTotal >= b.requiredFound;
        HexagonImageView badge = createHexBadgeView(grid, b.drawableRes, unlocked, getString(b.labelRes));
        badge.setOnClickListener(v -> showAchievementDetail(
                b.labelRes, b.detailRes, unlocked, false, 0, foundTotal, b.requiredFound));
        grid.addView(badge);
    }

    private HexagonImageView createHexBadgeView(GridLayout grid, int drawableRes, boolean unlocked,
            String contentDescription) {
        HexagonImageView badge = new HexagonImageView(requireContext());
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = dp(75);
        params.height = dp(75);
        params.setMargins(dp(3), dp(3), dp(3), dp(3));
        badge.setLayoutParams(params);
        setBadgeImage(badge, drawableRes);
        badge.setContentDescription(contentDescription);
        badge.setStrokeWidthDp(2f);
        badge.setStrokeColor(ContextCompat.getColor(requireContext(),
                unlocked ? R.color.ntuRed : R.color.ntu_support_text_muted));
        badge.setAlpha(unlocked ? 1f : 0.25f);
        if (unlocked) {
            badge.clearColorFilter();
        } else {
            badge.setColorFilter(Color.GRAY);
        }
        badge.setClickable(true);
        badge.setFocusable(true);
        return badge;
    }

    private void showAchievementDetail(int titleRes, int detailRes, boolean unlocked, boolean isWalkBadge,
            long walkedMeters, int treasuresFound, int requirement) {
        String body = getString(detailRes);
        if (unlocked) {
            body += "\n\n" + getString(R.string.achievement_status_unlocked);
        } else if (isWalkBadge) {
            int walkedShown = walkedMeters > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) walkedMeters;
            body += "\n\n" + getString(R.string.achievement_progress_walk, walkedShown, requirement);
        } else {
            body += "\n\n" + getString(R.string.achievement_progress_treasure, treasuresFound, requirement);
        }
        NtuMaterialDialogs.builder(requireContext())
                .setTitle(titleRes)
                .setMessage(body)
                .setPositiveButton(R.string.achievement_dialog_ok, null)
                .show();
    }

    /** Downsamples badge art via inSampleSize and caches bitmaps in {@link #BADGE_BITMAP_CACHE}. */
    private void setBadgeImage(@NonNull ImageView target, int drawableRes) {
        Bitmap cached = BADGE_BITMAP_CACHE.get(drawableRes);
        if (cached != null && !cached.isRecycled()) {
            target.setImageBitmap(cached);
            return;
        }

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(getResources(), drawableRes, bounds);

        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inPreferredConfig = Bitmap.Config.RGB_565;
        opts.inDither = true;
        opts.inSampleSize = computeInSampleSize(bounds, BADGE_TARGET_PX, BADGE_TARGET_PX);

        Bitmap bmp = BitmapFactory.decodeResource(getResources(), drawableRes, opts);
        if (bmp != null) {
            BADGE_BITMAP_CACHE.put(drawableRes, bmp);
            target.setImageBitmap(bmp);
        } else {
            target.setImageResource(drawableRes);
        }
    }

    private static int computeInSampleSize(@NonNull BitmapFactory.Options bounds, int reqWidth, int reqHeight) {
        int height = Math.max(1, bounds.outHeight);
        int width = Math.max(1, bounds.outWidth);
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return Math.max(1, inSampleSize);
    }

    /** Small square in “all treasures” grid; shows collected overlay when found. */
    private void addTreasureCell(GridLayout grid, Treasure treasure) {
        MaterialCardView wrapper = new MaterialCardView(requireContext());
        GridLayout.LayoutParams wrapParams = new GridLayout.LayoutParams();
        wrapParams.width = dp(65);
        wrapParams.height = dp(65);
        wrapParams.setMargins(dp(6), dp(6), dp(6), dp(6));
        wrapper.setLayoutParams(wrapParams);
        wrapper.setRadius(dp(12));
        wrapper.setCardElevation(0f);
        wrapper.setStrokeWidth(dp(1));
        wrapper.setStrokeColor(ColorStateList.valueOf(
                ContextCompat.getColor(requireContext(), R.color.ntu_leaderboard_row_stroke)));
        wrapper.setCardBackgroundColor(tileColor(treasure.getRarity()));
        wrapper.setPreventCornerOverlap(false);
        wrapper.setUseCompatPadding(false);

        ImageView slot = new ImageView(requireContext());
        FrameLayout.LayoutParams imgParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        slot.setLayoutParams(imgParams);
        slot.setScaleType(ImageView.ScaleType.CENTER_CROP);
        slot.setImageResource(R.drawable.greyplaceholder);
        boolean found = TreasureManager.isFound(requireContext(), treasure.getId());
        slot.setAlpha(found ? 1f : 0.4f);
        wrapper.addView(slot);

        if (found) {
            TextView badge = new TextView(requireContext());
            badge.setText(R.string.treasure_collected_badge);
            badge.setTextColor(Color.WHITE);
            badge.setTextSize(9f);
            badge.setTypeface(null, Typeface.BOLD);
            badge.setGravity(Gravity.CENTER);
            badge.setBackgroundColor(0xDD000000);
            FrameLayout.LayoutParams badgeParams = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT);
            badgeParams.gravity = Gravity.BOTTOM;
            badge.setPadding(2, 4, 2, 4);
            wrapper.addView(badge, badgeParams);
        }

        grid.addView(wrapper);
    }

    /** Card background by rarity (aligned with {@link TreasureFragment}). */
    private static int tileColor(Rarity rarity) {
        switch (rarity) {
            case COMMON:
                return Color.rgb(76, 175, 80);
            case RARE:
                return Color.rgb(33, 150, 243);
            case LEGENDARY:
                return Color.rgb(255, 193, 7);
            default:
                return Color.DKGRAY;
        }
    }

    /** Density-independent pixels → px for programmatic layouts. */
    private int dp(int d) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(d * density);
    }
}
