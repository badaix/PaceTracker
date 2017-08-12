package de.badaix.pacetracker.posprovider;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.LocationUtils;

abstract interface OnFixChangedListener {
    public void onFixChanged(boolean hasFix);
}

public class GpsPositionProvider extends PositionProvider {
    private static GpsPositionProvider instance = null;
    private LocationManager lm = null;
    private GpsStatus status = null;
    private boolean hasBroadcastFix = false;
    private long lastLocationMillis;
    private GpsFixReceiver fixReceiver;
    private IntentFilter filter;
    private Intent serviceIntent;

    GpsPositionProvider(Context context, boolean offline) {
        super(context, offline);
        Hint.log(this, "GpsPositionProvider");
        fixReceiver = new GpsFixReceiver(this);
        filter = new IntentFilter("android.location.GPS_FIX_CHANGE");
    }

    public static GpsPositionProvider getInstance(Context context, boolean offline) {
        if (instance == null) {
            instance = new GpsPositionProvider(context, offline);
        }
        instance.context = context;
        return instance;
    }


    @Override
    protected void startInternal() throws Exception {
        satCount = 0;
        fixCount = 0;
        lastLocationMillis = 0;
        lastLocation = null;
        fix = false;

        serviceIntent = new Intent(context, GpsService.class);
        context.startService(serviceIntent);

        lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            throw new Exception("GPS is disabled");
        }
        Hint.log(this, "GpsPositionProvider requestLocationUpdates");

        context.registerReceiver(fixReceiver, filter);
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        lm.addGpsStatusListener(this);
    }

    @Override
    protected void stopInternal() {
        Hint.log(this, "stop");
        try {
            context.unregisterReceiver(fixReceiver);
        } catch (Exception e) {
            Hint.log(this, e);
        }
        if (lm != null) {
            lm.removeGpsStatusListener(this);
            lm.removeUpdates(this);
        }
        context.stopService(serviceIntent);
        if (lastLocation != null)
            LocationUtils.setLastKnownLocation(lastLocation);
        instance = null;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null)
            return;
        lastLocationMillis = SystemClock.elapsedRealtime();
        if (lastLocation == null)
            LocationUtils.setLastKnownLocation(location);
        lastLocation = location;
        super.onLocationChanged(lastLocation);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        // TODO Auto-generated method stub
        if (event == GpsStatus.GPS_EVENT_FIRST_FIX)
            fix = true;
        else if (event == GpsStatus.GPS_EVENT_SATELLITE_STATUS) {
            if (!hasBroadcastFix && (lastLocation != null))
                fix = (SystemClock.elapsedRealtime() - lastLocationMillis) < 3000;
            fix |= hasBroadcastFix;

            status = lm.getGpsStatus(status);
            if (status == null)
                return;
            satCount = 0;
            fixCount = 0;

            Iterable<GpsSatellite> sats = status.getSatellites();
            for (GpsSatellite gps : sats) {
                satCount++;
                if (gps.usedInFix())
                    fixCount++;
            }

            // bFix = ((status.getTimeToFirstFix() > 0) && (iFixCount >= 2));
        }
        providerEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        super.onGpsStatusChanged(event);
    }

    @Override
    public void onFixChanged(boolean hasFix) {
        Hint.log(this, "Fix changed: " + hasFix);
        hasBroadcastFix = hasFix;
        fix |= hasBroadcastFix;
    }

    class GpsFixReceiver extends BroadcastReceiver {
        private OnFixChangedListener listener;
        private boolean fix = false;

        public GpsFixReceiver(OnFixChangedListener listener) {
            this.listener = listener;
        }

        public boolean hasFix() {
            return fix;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if ((listener != null) && "android.location.GPS_FIX_CHANGE".equals(intent.getAction())) {
                if (intent.getExtras() != null) {
                    fix = intent.getExtras().getBoolean("enabled", false);
                    listener.onFixChanged(fix);
                }
            }
        }
    }

}
