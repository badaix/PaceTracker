package de.badaix.pacetracker.maps;

import de.badaix.pacetracker.session.GeoPos;

public class MarkerInfo {
    public MarkerType markerType;

    ;
    public GeoPos pos;
    public String text;
    public String snipped;
    public BaseMarker marker;
    public String url;
    public MarkerInfo(MarkerType markerType, GeoPos pos, String text, String snipped) {
        this.markerType = markerType;
        this.pos = pos;
        this.text = text;
        this.snipped = snipped;
        marker = null;
    }

    public MarkerInfo(String url, GeoPos pos, String text, String snipped) {
        this.markerType = MarkerType.url;
        this.url = url;
        this.pos = pos;
        this.text = text;
        this.snipped = snipped;
        marker = null;
    }

    public void clear() {
        if (marker != null) {
            marker.clear();
            marker = null;
        }
    }

    public enum MarkerType {
        normal, start, finish, url;
    }
}
