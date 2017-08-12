package de.badaix.pacetracker.session;

public class GeoPos {
    public double latitude;
    public double longitude;
    public double distance = -1;

    public GeoPos(double lat, double lon) {
        latitude = lat;
        longitude = lon;
    }

    public GeoPos(double lat, double lon, double distance) {
        this(lat, lon);
        this.distance = distance;
    }

    @Override
    public int hashCode() {
        return (int) (Math.round(latitude) * Math.round(longitude));
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof GeoPos)) {
            return false; // different class
        }
        GeoPos other = (GeoPos) obj;
        if ((other.latitude == latitude) && (other.longitude == longitude))
            return true;

        return false;
    }

}
