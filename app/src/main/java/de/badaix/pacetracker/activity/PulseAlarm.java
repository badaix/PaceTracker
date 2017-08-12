package de.badaix.pacetracker.activity;

import android.content.Context;
import android.location.Location;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import java.util.Date;

import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.session.HxmData;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.PulseSettings;
import de.badaix.pacetracker.util.Hint;

public class PulseAlarm implements SessionUI {
    boolean active = true;
    private Context context;
    private MediaPlayer player;
    private long lastAlarm = new Date().getTime();
    private PulseSettings sensorSettings;

    public PulseAlarm(Context context) {
        this.context = context;
    }

    private void playAlarm() {
        if (!active)
            return;
        if ((player == null) || player.isPlaying())
            return;
        Date now = new Date();
        long lastPlay = now.getTime() - lastAlarm;
        Hint.log(this, "Last played: " + lastPlay);
        if (lastPlay < 7000)
            return;
        lastAlarm = now.getTime();
        player.start();
    }

    private void setActive(boolean active) {
        if ((sensorSettings == null) || (!sensorSettings.isMaxAlarmEnabled() && !sensorSettings.isMinAlarmEnabled())) {
            active = false;
            return;
        }
        Uri uri = GlobalSettings.getInstance().getPulseAlarmUri();
        if ((uri == null) || uri.equals(Uri.EMPTY)) {
            active = false;
            return;
        }
        player = new MediaPlayer();
        try {
            player.setDataSource(context, uri);
            player.setAudioStreamType(AudioManager.STREAM_RING);
            player.setLooping(false);
            player.prepare();
        } catch (Exception e) {
            Hint.log(this, e);
            player = null;
        }
        this.active = active;
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        if ((newState == State.RUNNING) || (newState == State.WAITSTART))
            setActive(true);
        else if (newState == State.STOPPED)
            setActive(false);
    }

    @Override
    public void onLocationChanged(Location location) {
    }

    @Override
    public void setSession(Session session) {
        sensorSettings = session.getSettings().getPulseSettings();
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        if (!active)
            return;

        Hint.log(this, "New sensor data: " + sensorData.getHeartRate());

        if (sensorSettings.isMaxAlarmEnabled() && (sensorData.getHeartRate() > sensorSettings.getMaxPulse()))
            playAlarm();
        else if (sensorSettings.isMinAlarmEnabled() && (sensorData.getHeartRate() < sensorSettings.getMinPulse()))
            playAlarm();
    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        setActive(active);
    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void onSessionCommand(int command) {
    }

    @Override
    public void update() {
    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
    }

}
