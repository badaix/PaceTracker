package de.badaix.pacetracker.maps;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

public abstract class BaseMarker {
    protected GoogleMap googleMap;
    protected Marker marker;

    public Marker getMarker() {
        return marker;
    }

    public void setVisible(boolean visible) {
        if (marker != null)
            marker.setVisible(visible);
    }

    public void clear() {
        if (marker != null)
            marker.remove();
        marker = null;
        googleMap = null;
    }
}
