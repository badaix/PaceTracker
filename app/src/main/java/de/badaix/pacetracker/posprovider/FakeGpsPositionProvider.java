package de.badaix.pacetracker.posprovider;

import android.content.Context;
import android.location.Location;

import java.util.Random;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.session.GeoPos;
import de.badaix.pacetracker.util.Distance;

public class FakeGpsPositionProvider extends PositionProvider implements PositionListener {
    private double lon, lat;
    private Location lastLoc;
    private Random random;
    private GpsPositionProvider gpsPositionProvider;

    public FakeGpsPositionProvider(Context context, boolean offline) {
        super(context, offline);
        gpsPositionProvider = GpsPositionProvider.getInstance(context, offline);
    }

    @Override
    protected void stopInternal() {
        gpsPositionProvider.stop(null);
    }

    @Override
    protected void startInternal() throws Exception {
        lon = 0;
        lat = 0;
        lastLoc = null;
        random = new Random();
        // super.startInternal();
        gpsPositionProvider.start(this);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (lastLoc == null) {
            lastLoc = location;
            lon = location.getLongitude();
            lat = location.getLatitude();
        }

        lon += 0.00004 + 0.000001 * random.nextDouble();
        lat += 0.00001 + 0.000001 * random.nextDouble();
        double bearing = Distance.bearing(new GeoPos(lastLoc.getLatitude(), lastLoc.getLongitude()), new GeoPos(lat,
                lon));
        location.setLongitude(lon);
        location.setLatitude(lat);
        location.setBearing((float) bearing);
        double dTmpDist = Distance.calculateDistance(lastLoc.getLatitude(), lastLoc.getLongitude(),
                location.getLatitude(), location.getLongitude(), Distance.METERS);
        location.setSpeed((float) dTmpDist);
        lastLoc = location;
        super.onLocationChanged(location);
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        this.providerEnabled = active;
        this.fix = hasFix;
        this.fixCount = fixCount;
        this.satCount = satCount;
        for (PositionListener pl : listener)
            pl.onGpsStatusChanged(active, hasFix, fixCount, satCount);
    }

}
