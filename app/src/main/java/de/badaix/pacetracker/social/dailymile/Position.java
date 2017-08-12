package de.badaix.pacetracker.social.dailymile;

public class Position {
    float latitude;
    float longitude;

    public Position(float latitude, float longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Position(android.location.Location location) {
        this.latitude = (float) location.getLatitude();
        this.longitude = (float) location.getLongitude();
    }
}
