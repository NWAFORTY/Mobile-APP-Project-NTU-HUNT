package com.example.treasurehuntapplication;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Single source of truth for hunt definitions (clues, GPS, rarity, points).
 * Collected state is stored per signed-in user in {@link SharedPreferences}.
 */
public final class TreasureManager {

    // --- Preferences: which treasure ids are "found" per user email ---
    private static final String PREF = "treasure_found_state";
    private static final String KEY_FOUND_SET_PREFIX = "found_ids_";

    private static final List<Treasure> TREASURES = new ArrayList<>();

    // --- Static hunt catalogue (id, copy, GPS, rarity, points) ---
    static {
        TREASURES.add(new Treasure(
                1,
                "Common",
                "I hold knowledge but make no sound. Find me where silence is loud.",
                "Look for water nearby, close to a spot where students play.",
                52.912014622528005, -1.1867476981535494,
                Treasure.Rarity.COMMON,
                10
        ));
        TREASURES.add(new Treasure(
                2,
                "Common",
                "Bats may swing and cheers may sound. Find the field where cricket’s found.",
                "Stay on the grass and search near the edge of the playing area.",
                52.9143221975777, -1.1846936807536121,
                Treasure.Rarity.COMMON,
                10
        ));
        TREASURES.add(new Treasure(
                3,
                "Rare",
                "Sticks may clash and goals are chased. Find the turf where hockey’s placed.",
                "Look along the lines where players wait to sub on and cheer.",
                52.91253722913912, -1.1888147758766068,
                Treasure.Rarity.RARE,
                25
        ));
        TREASURES.add(new Treasure(
                4,
                "Rare",
                "Ideas rise in glass and steel. Find the STEM building where futures feel real.",
                "Search near the busiest entrance where people come and go.",
                52.911170433353334, -1.1845803402665962,
                Treasure.Rarity.RARE,
                25
        ));
        TREASURES.add(new Treasure(
                5,
                "Rare",
                "Curious minds and lessons meet. Find the School of Science & Technology seat.",
                "Try the main doors—where the building first welcomes you in.",
                52.91055489051997, -1.1866600260573863,
                Treasure.Rarity.RARE,
                25
        ));
        TREASURES.add(new Treasure(
                6,
                "Legendary",
                "Voices gather, history stays. Seek the old stone by Village Hall’s ways.",
                "Find the old stone close to a place where people gather for events.",
                52.90843613936876, -1.1870923367236603,
                Treasure.Rarity.LEGENDARY,
                100
        ));
        TREASURES.add(new Treasure(
                7,
                "Legendary",
                "Mirrors, chairs, and careful hands—find the beautification parlour where style expands.",
                "Stand where the parlour meets the pavement; the treasure waits near the beautician’s door.",
                52.95562325166844, -1.1567528670292193,
                Treasure.Rarity.LEGENDARY,
                100
        ));
    }

    private TreasureManager() {
    }

    // --- Preference key helpers (scoped by UserSession email) ---

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREF, Context.MODE_PRIVATE);
    }

    private static String safeKey(String raw) {
        return raw.trim().toLowerCase().replaceAll("[^a-z0-9]+", "_");
    }

    private static String foundSetKey(Context context) {
        String email = UserSession.getEmail(context.getApplicationContext());
        if (email == null || email.isEmpty()) {
            return KEY_FOUND_SET_PREFIX + "guest";
        }
        return KEY_FOUND_SET_PREFIX + safeKey(email);
    }

    // --- Per-user collection state ---

    public static boolean isFound(Context context, int treasureId) {
        Set<String> set = prefs(context).getStringSet(foundSetKey(context), Collections.emptySet());
        return set.contains(String.valueOf(treasureId));
    }

    public static void markFound(Context context, int treasureId) {
        Set<String> next = new HashSet<>(prefs(context).getStringSet(foundSetKey(context), Collections.emptySet()));
        next.add(String.valueOf(treasureId));
        prefs(context).edit().putStringSet(foundSetKey(context), next).apply();
    }

    /** Removes one treasure from the collected set (e.g. debug / retesting a location). */
    public static void unmarkFound(Context context, int treasureId) {
        Set<String> next = new HashSet<>(prefs(context).getStringSet(foundSetKey(context), Collections.emptySet()));
        next.remove(String.valueOf(treasureId));
        prefs(context).edit().putStringSet(foundSetKey(context), next).apply();
    }

    public static int getFoundCount(Context context) {
        return prefs(context).getStringSet(foundSetKey(context), Collections.emptySet()).size();
    }

    /** Ordered ids of treasures this user has collected (for Firestore sync). */
    public static List<Long> getFoundTreasureIdsForSync(Context context) {
        Set<String> raw = prefs(context).getStringSet(foundSetKey(context), Collections.emptySet());
        List<Long> out = new ArrayList<>();
        for (String s : raw) {
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException ignored) {
                // skip
            }
        }
        Collections.sort(out);
        return out;
    }

    // --- Read-only access to catalogue ---

    public static List<Treasure> getAll() {
        return Collections.unmodifiableList(TREASURES);
    }

    public static List<Treasure> getByRarity(Treasure.Rarity rarity) {
        List<Treasure> out = new ArrayList<>();
        for (Treasure t : TREASURES) {
            if (t.getRarity() == rarity) {
                out.add(t);
            }
        }
        return out;
    }

    public static Treasure getById(int id) {
        for (Treasure t : TREASURES) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    public static Treasure getNextTreasure(Context context) {
        for (Treasure t : TREASURES) {
            if (!isFound(context, t.getId())) {
                return t;
            }
        }
        return null;
    }

    public static int getTotalCount() {
        return TREASURES.size();
    }

    public static int countByRarity(Treasure.Rarity rarity) {
        int n = 0;
        for (Treasure t : TREASURES) {
            if (t.getRarity() == rarity) {
                n++;
            }
        }
        return n;
    }

    // --- Maintenance (device-wide) ---

    /** Clears collected-treasure flags for every account on this device. */
    public static void clearAllCollectedState(Context context) {
        prefs(context).edit().clear().apply();
    }
}
