package de.badaix.pacetracker.posprovider;

import android.content.Context;
import android.location.Location;

import java.util.Date;
import java.util.Random;

import de.badaix.pacetracker.PositionListener;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.OnTimerListener;
import de.badaix.pacetracker.util.Timer;

public class FakePositionProvider extends PositionProvider implements OnTimerListener {
    private Timer gpsFakeTimer;
    private Location location;
    private double gpsLon = 6.104278564453125;
    private double gpsLat = 50.77981173992157;
    private double bearing = 0.0;
    private Random random = new Random();

    public FakePositionProvider(Context context, boolean offline) {
        super(context, offline);
    }

    @Override
    protected void stopInternal() {
        if (gpsFakeTimer != null) {
            gpsFakeTimer.cancel(true);
            gpsFakeTimer = null;
        }
    }

    @Override
    protected void startInternal() throws Exception {
        gpsFakeTimer = new Timer(100);
        gpsFakeTimer.execute(this);
    }

    @Override
    public void onTimer() {
        gpsLon += 0.00001 + random.nextDouble() * 0.00001;
        gpsLat += 0.00001 + random.nextDouble() * 0.00001;
        bearing += 1.5;
        Hint.log(this, "FakeGps: " + gpsLon);

        location = new Location("GPS");
        location.setLongitude(gpsLon);
        location.setLatitude(gpsLat);
        location.setAltitude(10.);
        Date now = new Date();
        location.setTime(now.getTime());
        location.setBearing((int) bearing);
        location.setSpeed(0.1f / 3.6f);
        for (PositionListener pl : listener) {
            pl.onLocationChanged(location);
            pl.onGpsStatusChanged(true, true, 5, 6);
        }
    }
}
