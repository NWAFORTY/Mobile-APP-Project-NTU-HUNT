package com.example.treasurehuntapplication;

/**
 * Hunt definition (clue, GPS, rarity, points). Whether the player has collected it is stored
 * per-user in {@link TreasureManager}, not on this object.
 */
public class Treasure {

    /** Hunt difficulty tier (drives UI colour and point value in catalogue). */
    public enum Rarity {
        COMMON,
        RARE,
        LEGENDARY
    }

    // --- Immutable hunt definition ---
    private final int id;
    private final String title;
    private final String clue;
    private final String hint;
    private final double latitude;
    private final double longitude;
    private final Rarity rarity;
    private final int points;

    /** Full constructor used by {@link TreasureManager} static catalogue. */
    public Treasure(int id, String title, String clue, String hint,
                    double latitude, double longitude, Rarity rarity, int points) {
        this.id = id;
        this.title = title;
        this.clue = clue;
        this.hint = hint;
        this.latitude = latitude;
        this.longitude = longitude;
        this.rarity = rarity;
        this.points = points;
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getClue() {
        return clue;
    }

    public String getHint() {
        return hint;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Rarity getRarity() {
        return rarity;
    }

    public int getPoints() {
        return points;
    }
}
