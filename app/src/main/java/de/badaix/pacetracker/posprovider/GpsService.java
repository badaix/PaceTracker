package de.badaix.pacetracker.posprovider;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import de.badaix.pacetracker.util.RefCountWakeLock;

public class GpsService extends Service {

    @Override
    public void onCreate() {
        RefCountWakeLock.getInstance(this).acquire(this);
    }

    @Override
    public void onDestroy() {
        RefCountWakeLock.getInstance(this).release(this);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
