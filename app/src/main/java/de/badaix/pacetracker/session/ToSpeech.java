package de.badaix.pacetracker.session;

import android.content.Context;
import android.location.Location;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

import java.text.DecimalFormat;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.sensor.SensorData;
import de.badaix.pacetracker.sensor.SensorProvider;
import de.badaix.pacetracker.sensor.SensorProvider.SensorState;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.settings.VoiceFeedbackSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Hint;
import de.badaix.pacetracker.util.TTS;

public class ToSpeech extends PhoneStateListener implements SessionUI {
    boolean active = true;
    private Session session;
    // private long lLastKilometerDuration = -1;
    private double dNextKilometer;
    private long lNextDuration;
    private GpsPos lastKilometer;
    private GpsPos lastDuration;
    private VoiceFeedbackSettings settings;
    private DecimalFormat formatter;
    private Context context;
    private TelephonyManager telephonyManager;

    public ToSpeech(Context context) {
        this.context = context;
        settings = GlobalSettings.getInstance(context).getVoiceFeedback();
        formatter = new DecimalFormat("#.#");
        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        super.onCallStateChanged(state, incomingNumber);
        if (state != TelephonyManager.CALL_STATE_IDLE)
            TTS.getInstance().interrupt();
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    private String Duration(long ms) {
        String[] vTime = DateUtils.secondsToHHMMSSString(ms / 1000).split(":");

        String message = "";
        int iTmp = Integer.parseInt(vTime[0]);
        if (iTmp > 1)
            message = iTmp + " " + context.getString(R.string.hours) + " ";
        else if (iTmp == 1)
            message = context.getString(R.string.oneHour) + " ";

        iTmp = Integer.parseInt(vTime[1]);
        if (iTmp > 1)
            message += iTmp + " " + context.getString(R.string.minutes) + " ";
        else if (iTmp == 1)
            message += " " + context.getString(R.string.oneMinute) + " ";

        iTmp = Integer.parseInt(vTime[2]);
        if (iTmp > 1)
            message += iTmp + " " + context.getString(R.string.seconds);
        else if (iTmp == 1)
            message += " " + context.getString(R.string.oneSecond);

        return message;
    }

    public void speak(String message) {
        speak(message, false);
    }

    public void speak(String message, boolean flush) {
        Hint.log(this, "Speak: " + message + ", active: " + active);
        if (!active)
            return;
        TTS.getInstance().speak(message, flush);
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        if ((newState == State.RUNNING) && ((oldState == State.WAITSTART) || (oldState == State.INIT))) {
            speak("start");
            lastDuration = null;
            lastKilometer = null;
            dNextKilometer = settings.distanceInterval * GlobalSettings.getInstance().getDistUnit().getFactor() / 1000.;
            lNextDuration = settings.durationInterval * 1000;
        } else if (newState == State.STOPPED) {
            // speak("stop", true);
            telephonyManager.listen(this, LISTEN_NONE);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub

    }

    @Override
    public void setSession(Session session) {
        this.session = session;
    }

    @Override
    public void onGpsStatusChanged(boolean active, boolean hasFix, int fixCount, int satCount) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorData(SensorProvider provider, SensorData sensorData) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorStateChanged(SensorProvider provider, boolean active, SensorState sensorState) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onSensorDataChanged(HxmData hxmData) {
    }

    @Override
    public void onSessionCommand(int command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void update() {
        // TODO Auto-generated method stub

    }

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        if (lastDuration == null)
            lastDuration = location;
        if (lastKilometer == null)
            lastKilometer = location;

        // Hint.log(this, "Duration: " + location.duration + ", next: "
        // + lNextDuration);

        if (settings.isDistanceEnabled()) {
            if (location.distance >= dNextKilometer) {
                dNextKilometer += settings.distanceInterval * GlobalSettings.getInstance().getDistUnit().getFactor()
                        / 1000.;

                long lLastKilometerDuration = location.duration - lastKilometer.duration;
                double distance = session.getDistance() / GlobalSettings.getInstance().getDistUnit().getFactor();

                String msg = "";
                if (settings.speakDistTotal)
                    msg += formatter.format(distance)
                            + " "
                            + GlobalSettings.getInstance().getDistUnit()
                            .toLocaleString(context, (Math.floor(distance) == 1)) + ", ";
                if (settings.speakDistDuration)
                    msg += Duration(lLastKilometerDuration) + ", ";
                if (settings.speakDistTotalDuration)
                    msg += Duration(location.duration);

                speak(msg);

                lastKilometer = location;
            }
        }

        if (settings.isDurationEnabled()) {
            if (location.duration >= lNextDuration) {
                lNextDuration += settings.durationInterval * 1000;
                double lastDistance = (location.distance - lastDuration.distance)
                        / GlobalSettings.getInstance().getDistUnit().getFactor();
                double distance = session.getDistance() / GlobalSettings.getInstance().getDistUnit().getFactor();

                String msg = "";
                if (settings.speakDurationTotal)
                    msg += Duration(location.duration) + ", ";
                if (settings.speakDurationDistance)
                    msg += formatter.format(lastDistance)
                            + " "
                            + GlobalSettings.getInstance().getDistUnit()
                            .toLocaleString(context, (Math.floor(lastDistance) == 1)) + ", ";
                if (settings.speakDurationTotalDistance)
                    msg += formatter.format(distance)
                            + " "
                            + GlobalSettings.getInstance().getDistUnit()
                            .toLocaleString(context, (Math.floor(distance) == 1));

                speak(msg);

                lastDuration = location;
            }
        }
    }

}
