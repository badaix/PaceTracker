package de.badaix.pacetracker.util;

import java.util.Vector;

import de.badaix.pacetracker.session.GeoPos;

public class BoundingBox {
    double minLat = -1;
    double minLon = -1;
    double maxLat = -1;
    double maxLon = -1;
    boolean initialized = false;

    public BoundingBox(double minLat, double minLon, double maxLat, double maxLon) {
        this();
        add(new GeoPos(minLat, minLon));
        add(new GeoPos(maxLat, maxLon));
    }

    public BoundingBox() {
        initialized = false;
    }

    public <T extends GeoPos> BoundingBox(Vector<T> geoPositions) {
        this();
        for (T geoPos : geoPositions)
            add(geoPos);
    }

    public BoundingBox(BoundingBox boundingBox) {
        this.maxLat = boundingBox.getMaxLat();
        this.maxLon = boundingBox.getMaxLon();
        this.minLat = boundingBox.getMinLat();
        this.minLon = boundingBox.getMinLon();
        this.initialized = boundingBox.isInitialized();
    }

    public double getMinLat() {
        return minLat;
    }

    public double getMinLon() {
        return minLon;
    }

    // public BoundingBoxE6 getBoundingBoxE6() {
    // return new BoundingBoxE6(maxLat, maxLon, minLat, minLon);
    // }

    public double getMaxLat() {
        return maxLat;
    }

    public double getMaxLon() {
        return maxLon;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public GeoPos getCenter() {
        return new GeoPos((minLat + (maxLat - minLat) / 2.0), (minLon + (maxLon - minLon) / 2.0));
    }

    public boolean isInside(GeoPos geoPos) {
        if (!initialized)
            return false;

        return ((geoPos.latitude >= minLat) && (geoPos.latitude <= maxLat) && (geoPos.longitude >= minLon) && (geoPos.longitude <= maxLon));
    }

    public void add(GeoPos geoPos) {
        if (!initialized) {
            minLat = geoPos.latitude;
            minLon = geoPos.longitude;
            maxLat = minLat;
            maxLon = minLon;
            initialized = true;
            return;
        }

        if (geoPos.latitude < minLat)
            minLat = geoPos.latitude;
        else if (geoPos.latitude > maxLat)
            maxLat = geoPos.latitude;

        if (geoPos.longitude < minLon)
            minLon = geoPos.longitude;
        else if (geoPos.longitude > maxLon)
            maxLon = geoPos.longitude;
    }
}
