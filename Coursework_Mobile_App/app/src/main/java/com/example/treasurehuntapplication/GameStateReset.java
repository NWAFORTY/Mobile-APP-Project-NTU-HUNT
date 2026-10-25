package com.example.treasurehuntapplication;

import android.content.Context;

/** Clears local hunt progress for shipped data migrations. */
public final class GameStateReset {

    private GameStateReset() {
    }

    public static void clearLocalGameProgress(Context context) {
        Context app = context.getApplicationContext();
        TreasureManager.clearAllCollectedState(app);
        ActiveTreasureManager.clearAllActiveTreasureState(app);
        WalkDistanceTracker.clearAll(app);
        ScoreRepository.clearAll(app);
    }
}
