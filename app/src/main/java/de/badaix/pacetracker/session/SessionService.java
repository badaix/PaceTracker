package de.badaix.pacetracker.session;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.ActivitySession;
import de.badaix.pacetracker.activity.PulseAlarm;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.SessionSettings;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.RefCountWakeLock;
import de.badaix.pacetracker.util.TTS;

public class SessionService extends Service implements SessionListener {
    private final IBinder mBinder = new LocalBinder();
    private Session session;
    private SessionSettings sessionSettings;
    private SessionWriter sessionWriter;
    private ToSpeech toSpeech;
    private PulseAlarm pulseAlarm;
    private Vector<SessionUI> vSessionUIs;
    private SessionListener listener = null;
    private boolean started;

    @Override
    public void onCreate() {
        Hint.log(this, "NotificationService.onCreate()");
        started = false;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.session_notification).setTicker("Session started")
                .setContentTitle("Pace tracker").setContentText("Session running");

        Intent sessionIntent = new Intent(this, ActivitySession.class);
        sessionIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0, sessionIntent, 0);
        mBuilder.setContentIntent(contentIntent);
        startForeground(R.string.app_name, mBuilder.build());
        TTS.getInstance().init(this);

        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        TTS.getInstance().shutdown();
        stopForeground(true);
    }

    public void setListener(SessionListener listener) {
        this.listener = listener;
    }

    public Session startSession(SessionSettings settings) {
        if (started)
            return null;

        GlobalSettings.getInstance().service = this;
        try {
            started = true;
            this.sessionSettings = settings;
            session = SessionFactory.getInstance().getSessionByType(settings.getSessionType(), this, sessionSettings);

            pulseAlarm = new PulseAlarm(this);
            toSpeech = new ToSpeech(this);
            toSpeech.setActive(settings.isVoiceFeedback());
            sessionWriter = new SessionWriter(this);
            vSessionUIs = new Vector<SessionUI>();
            vSessionUIs.add(toSpeech);
            vSessionUIs.add(pulseAlarm);
            vSessionUIs.add(sessionWriter);
            if (settings.getGoal() != null)
                vSessionUIs.add(settings.getGoal());

            for (SessionUI s : vSessionUIs) {
                s.setSession(session);
            }

            RefCountWakeLock.getInstance(this).acquire(this);
            session.startSession();
        } catch (Exception e) {
            started = false;
            RefCountWakeLock.getInstance(this).release(this);
            e.printStackTrace();
        }
        return session;
    }

    public void stopSession(boolean discard) {
        if (!started)
            return;
        GlobalSettings.getInstance().service = null;
        session.stopSession();
        session.closeSession(discard);
        if (!discard)
            GlobalSettings.getInstance().setSessionSummary(session);
        started = false;
        RefCountWakeLock.getInstance(this).release(this);
        stopSelf();
    }

    public Session getSession() {
        return session;
    }

    public void enableTts(boolean enabled) {
        toSpeech.setActive(enabled);
    }

    @Override
    public void onLocationChanged(Location location) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onLocationChanged(location);
        if (listener != null) {
            try {
                listener.onLocationChanged(location);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onGpsStatusChanged(active, hasFix, fixCount, satCount);
        if (listener != null) {
            try {
                listener.onGpsStatusChanged(active, hasFix, fixCount, satCount);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onSensorData(provider, sensorData);
        if (listener != null) {
            try {
                listener.onSensorData(provider, sensorData);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onSensorStateChanged(provider, active, sensorState);
        if (listener != null) {
            try {
                listener.onSensorStateChanged(provider, active, sensorState);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onStateChanged(oldState, newState);
        if (listener != null) {
            try {
                listener.onStateChanged(oldState, newState);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onSessionCommand(int command) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onSessionCommand(command);
        if (listener != null) {
            try {
                listener.onSessionCommand(command);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onFilteredLocationChanged(location);
        if (listener != null) {
            try {
                listener.onFilteredLocationChanged(location);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
        for (SessionUI sessionUI : vSessionUIs)
            sessionUI.onSensorDataChanged(hxmData);
        if (listener != null) {
            try {
                listener.onSensorDataChanged(hxmData);
            } catch (Exception e) {
                Hint.log(this, e);
            }
        }
    }

    public class LocalBinder extends Binder {
        public SessionService getService() {
            return SessionService.this;
        }
    }
}
