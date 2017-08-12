package de.badaix.pacetracker.util;

import android.content.Context;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import java.util.HashSet;
import java.util.Set;

public class RefCountWakeLock {
    private static RefCountWakeLock instance = null;
    private Context ctx = null;
    private Set<Object> objects;
    private WakeLock wakeLock = null;

    public static RefCountWakeLock getInstance(Context context) {
        if (instance == null) {
            instance = new RefCountWakeLock();
            instance.objects = new HashSet<Object>();
        }
        instance.ctx = context;
        return instance;
    }

    public void release(Object object) {
        if (!objects.contains(object))
            return;

        objects.remove(object);
        if (objects.isEmpty() && (wakeLock != null) && wakeLock.isHeld()) {
            Hint.log(this, "Wake lock released");
            wakeLock.release();
            wakeLock = null;
        }
    }

    public void acquire(Object object) {
        try {
            PowerManager powerManager = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
            if (powerManager == null) {
                Hint.log(this, "powerManager is null.");
                return;
            }
            if (wakeLock == null) {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, this.getClass().getSimpleName());
                if (wakeLock == null) {
                    Hint.log(this, "wakeLock is null.");
                    return;
                }
            }
            objects.add(object);
            Hint.log(this, "Wake lock acquire - ojects: " + objects.size());
            if (!wakeLock.isHeld()) {
                wakeLock.acquire();
                if (!wakeLock.isHeld()) {
                    objects.clear();
                    Hint.log(this, "Unable to hold wakeLock.");
                }
            }
        } catch (RuntimeException e) {
            Hint.log(this, e);
        }
    }

}
