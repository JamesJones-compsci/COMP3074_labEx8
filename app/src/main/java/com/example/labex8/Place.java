package com.example.labex8;

/**
 * Represents a favorite place selected by the user.
 * This class will be serialized and stored in SharedPreferences using Gson.
 */
public class Place {
    private String title;
    private double latitude;
    private double longitude;

    // Constructor
    public Place(String title, double latitude, double longitude) {
        this.title = title;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters
    public String getTitle() {
        return title;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    // For displaying in ListView
    @Override
    public String toString() {
        return title + "\n(" + latitude + ", " + longitude + ")";
    }
}
