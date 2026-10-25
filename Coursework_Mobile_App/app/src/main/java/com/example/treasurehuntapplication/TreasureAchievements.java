package com.example.treasurehuntapplication;

/** Treasure-count achievement definitions for the stats hex grid. */
public final class TreasureAchievements {

    public static final class Badge {
        public final int drawableRes;
        public final int requiredFound;
        public final int labelRes;
        public final int detailRes;

        public Badge(int drawableRes, int requiredFound, int labelRes, int detailRes) {
            this.drawableRes = drawableRes;
            this.requiredFound = requiredFound;
            this.labelRes = labelRes;
            this.detailRes = detailRes;
        }
    }

    private TreasureAchievements() {
    }

    public static Badge[] getTreasureBadges() {
        return new Badge[]{
                new Badge(R.drawable.treasure_1, 1, R.string.achievement_treasure_1,
                        R.string.achievement_treasure_1_detail),
                new Badge(R.drawable.treasure_3, 3, R.string.achievement_treasure_3,
                        R.string.achievement_treasure_3_detail),
                new Badge(R.drawable.treasure_5, 5, R.string.achievement_treasure_5,
                        R.string.achievement_treasure_5_detail),
        };
    }
}
