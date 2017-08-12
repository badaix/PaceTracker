package de.badaix.pacetracker.posprovider;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;

public class PositionProviderService extends Service {
    private final IBinder mBinder = new LocalBinder();
    private PositionProvider positionProvider;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        stopSelf();
    }

    public void startProvider(PositionProvider provider) {
        this.positionProvider = provider;

    }

    public PositionProvider getProvider() {
        return positionProvider;
    }

    public class LocalBinder extends Binder {
        public PositionProviderService getService() {
            return PositionProviderService.this;
        }
    }

}
