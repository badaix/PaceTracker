package de.badaix.pacetracker;

import android.location.Location;

public abstract interface PositionListener {
    public void onLocationChanged(Location location);

    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount);
}
