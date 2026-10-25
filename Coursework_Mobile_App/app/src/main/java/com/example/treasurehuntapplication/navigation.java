package com.example.treasurehuntapplication;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.treasurehuntapplication.databinding.ActivityNavigationBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.material.navigation.NavigationBarView;

/**
 * Main app shell after login: bottom navigation, fragment swapping (Map, Treasure, Leaderboard,
 * Profile, Support), session sync with Firebase, one-time game-data migration, location permission,
 * and first-run tutorial per user.
 */
public class navigation extends AppCompatActivity {

    private static final int REQ_LOCATION = 100;

    // --- One-time local + remote game reset (versioned) ---
    /**
     * Bump when shipping a full reset of local + (for Firebase users) remote game fields for all installs.
     */
    private static final int GAME_DATA_VERSION = 2;
    private static final String BOOTSTRAP_PREF = "app_bootstrap";
    private static final String KEY_GAME_DATA_VERSION = "game_data_version";

    ActivityNavigationBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // --- Align UserSession with Firebase if app was restored with persisted Auth ---
        syncSessionWithFirebaseAuth();
        // --- Optional shipped reset: clear prefs + wipe current user’s Firestore game fields once ---
        applyGameDataMigrationIfNeeded();
        // --- Pull display name from Firestore when account changes ---
        UserProfileFirestoreSync.hydrateDisplayNameForCurrentAccount(this);
        // --- Not signed in → back to Login ---
        if (!UserSession.isSignedIn(this)) {
            Intent login = new Intent(this, Login.class);
            login.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(login);
            finish();
            return;
        }

        // --- Ensure users/{email} exists for leaderboard visibility ---
        LeaderboardUserRegistration.ensureRegistered(this);

        EdgeToEdge.enable(this);

        // --- Map / hunt needs location ---
        requestLocationIfNeeded();

        binding = ActivityNavigationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavigationBarView navBar = binding.Navbar;
        navBar.setItemActiveIndicatorEnabled(true);
        navBar.setItemActiveIndicatorColor(
                ColorStateList.valueOf(ContextCompat.getColor(this, R.color.ntu_nav_indicator)));

        // --- First launch per account: tutorial after Map is laid out (dimmed map behind) ---
        binding.getRoot().post(() -> TutorialPopup.showIfFirstTime(this));

        // --- Edge-to-edge insets on main content ---
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            Insets cutout = windowInsets.getInsets(WindowInsetsCompat.Type.displayCutout());
            v.setPadding(
                    Math.max(bars.left, cutout.left),
                    Math.max(bars.top, cutout.top),
                    Math.max(bars.right, cutout.right),
                    Math.max(bars.bottom, cutout.bottom));
            return windowInsets;
        });

        // --- Initial tab: Map ---
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();
            binding.Navbar.setSelectedItemId(R.id.map);
        }

        // --- Bottom nav: swap fragments ---
        binding.Navbar.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.profile) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new ProfileFragment())
                        .commit();
            } else if (itemId == R.id.leaderboard) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new LeaderboardFragment())
                        .commit();
            } else if (itemId == R.id.map) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new MapFragment())
                        .commit();
            } else if (itemId == R.id.treasure) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new TreasureFragment())
                        .commit();
            } else if (itemId == R.id.support) {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new SupportFragment())
                        .commit();
            }
            return true;
        });
    }

    /** One-time wipe of local progress and (for Firebase users) remote score / walk / treasures. */
    private void applyGameDataMigrationIfNeeded() {
        SharedPreferences p = getSharedPreferences(BOOTSTRAP_PREF, MODE_PRIVATE);
        int stored = p.getInt(KEY_GAME_DATA_VERSION, 0);
        if (stored >= GAME_DATA_VERSION) {
            return;
        }
        GameStateReset.clearLocalGameProgress(this);
        UserGameStateFirestoreSync.wipeRemoteGameFieldsForCurrentUser(this);
        p.edit().putInt(KEY_GAME_DATA_VERSION, GAME_DATA_VERSION).apply();
    }

    /** Keeps local session aligned when the user signed in with Firebase Auth. */
    private void syncSessionWithFirebaseAuth() {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        if (u != null && u.getEmail() != null && !u.getEmail().isEmpty()) {
            UserSession.signIn(this, u.getEmail());
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // --- Session + profile may change while app is backgrounded ---
        syncSessionWithFirebaseAuth();
        UserProfileFirestoreSync.hydrateDisplayNameForCurrentAccount(this);
        if (UserSession.isSignedIn(this)) {
            LeaderboardUserRegistration.ensureRegistered(this);
            UserGameStateFirestoreSync.pushGameStateToFirestore(this);
        }
    }

    /** Requests fine/coarse location if not already granted (treasure hunt map). */
    private void requestLocationIfNeeded() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    REQ_LOCATION);
        }
    }

    /** After accepting a treasure, jump to Map so GPS hunt continues there. */
    public void showMapTab() {
        if (binding.Navbar.getSelectedItemId() == R.id.map) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new MapFragment())
                    .commit();
        } else {
            binding.Navbar.setSelectedItemId(R.id.map);
        }
    }
}
