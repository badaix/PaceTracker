package de.badaix.pacetracker.posprovider;

import android.content.Context;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import org.json.JSONObject;

import java.util.HashSet;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.util.Hint;

public abstract class PositionProvider implements Listener, LocationListener, OnFixChangedListener {
    protected Context context;
    protected boolean hasLocationInfo = true;
    protected boolean isResumed = false;
    protected boolean offline = false;
    protected HashSet<PositionListener> listener = new HashSet<PositionListener>();

    protected boolean fix = false;
    protected boolean providerEnabled = false;
    protected int satCount = 0;
    protected int fixCount = 0;
    protected Location lastLocation;

    // protected static PositionProvider instance = null;

    PositionProvider(Context context, boolean offline) {
        this.context = context;
        this.offline = offline;
    }

    public boolean isOffline() {
        return offline;
    }

    protected JSONObject getSettings() {
        return new JSONObject();
    }

    protected void putSettings(JSONObject json) {
    }

    public boolean hasLocationInfo() {
        return this.hasLocationInfo;
    }

    final public String getName() {
        return this.getClass().getSimpleName();
    }

    // public abstract GpsStatus getGpsStatus(GpsStatus status);
    protected abstract void startInternal() throws Exception;

    protected abstract void stopInternal();

    final public void start(PositionListener listener) throws Exception {
        this.listener.add(listener);
        if (!isResumed) {
            startInternal();
            isResumed = true;
        }
    }

    final public void stop(PositionListener listener) {
        if (isResumed) {
            if (listener == null)
                this.listener.clear();
            else
                this.listener.remove(listener);

            if (this.listener.isEmpty()) {
                stopInternal();
                isResumed = false;
            }
        }
    }

    @Override
    public void onFixChanged(boolean hasFix) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onLocationChanged(Location location) {
        synchronized (listener) {
            for (PositionListener pl : listener)
                pl.onLocationChanged(location);
        }
    }

    @Override
    public void onProviderDisabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER))
            synchronized (listener) {
                for (PositionListener pl : listener)
                    pl.onGpsStatusChanged(false, fix, fixCount, satCount);
            }
    }

    @Override
    public void onProviderEnabled(String provider) {
        Hint.log(this, "onProviderEnabled: " + provider);
        if (provider.equals(LocationManager.GPS_PROVIDER))
            synchronized (listener) {
                for (PositionListener pl : listener)
                    pl.onGpsStatusChanged(true, fix, fixCount, satCount);
            }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onGpsStatusChanged(int event) {
        synchronized (listener) {
            for (PositionListener pl : listener)
                pl.onGpsStatusChanged(providerEnabled, fix && (satCount >= 3), fixCount, satCount);
        }
    }

    public boolean isFix() {
        return fix;
    }

    public int getSatCount() {
        return satCount;
    }

    public int getFixCount() {
        return fixCount;
    }

    public Location getLastLocation() {
        return lastLocation;
    }

    public boolean isProviderEnabled() {
        return providerEnabled;
    }

}
