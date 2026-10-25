package com.example.treasurehuntapplication;

/** Walk-distance achievement definitions for the stats hex grid. */
public final class WalkAchievements {

    public static final class Badge {
        public final int drawableRes;
        public final int requiredMeters;
        public final int labelRes;
        /** Long description shown when the user taps the badge. */
        public final int detailRes;

        public Badge(int drawableRes, int requiredMeters, int labelRes, int detailRes) {
            this.drawableRes = drawableRes;
            this.requiredMeters = requiredMeters;
            this.labelRes = labelRes;
            this.detailRes = detailRes;
        }
    }

    private WalkAchievements() {
    }

    public static Badge[] getWalkBadges() {
        return new Badge[]{
                new Badge(R.drawable.achievement_10m, 10, R.string.achievement_walk_10m,
                        R.string.achievement_walk_10m_detail),
                new Badge(R.drawable.achievement_50m, 100, R.string.achievement_walk_100m,
                        R.string.achievement_walk_100m_detail),
                new Badge(R.drawable.achievement_100m, 500, R.string.achievement_walk_500m,
                        R.string.achievement_walk_500m_detail),
        };
    }
}
