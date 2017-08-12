package de.badaix.pacetracker.session;

public class GpsPos extends GeoPos {
    public double altitude;
    public long time;
    public long duration;
    public float speed;
    public float bearing;

    public GpsPos(double lat, double lon, double alt, long time, float speed, float bearing, long duration,
                  double distance) {
        super(lat, lon, distance);
        this.altitude = alt;
        this.time = time;
        this.bearing = bearing;
        this.speed = speed;
        this.duration = duration;
    }

    // public GpsPos(Location location, long duration, double distance) {
    // super(location.getLatitude(), location.getLongitude(), distance);
    // altitude = location.getAltitude();
    // time = location.getTime();
    // bearing = location.getBearing();
    // speed = location.getSpeed();
    // this.duration = duration;
    // }
}
