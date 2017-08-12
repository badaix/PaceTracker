package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.preference.Preference;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.FragmentSessionOfflineOverview;
import de.badaix.pacetracker.activity.FragmentSessionOverview;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.preferences.DurationPreference;
import de.badaix.pacetracker.session.Session;
import de.badaix.pacetracker.session.Session.State;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.TTS;
import de.badaix.pacetracker.views.OverviewItem;

public class GoalDuration extends Goal {
    private long duration = 0;
    private long durationLeft = 0;
    private OverviewItem overviewItem;
    private DurationPreference durationPreference;
    private double finishedAfter = -1;
    private Timer timer;
    private Vector<Pair<String, String>> items;
    private TimeLeftTimer timeLeftTimer;

    GoalDuration(Activity activity) {
        super(activity);
        timer = new Timer();
        timeLeftTimer = new TimeLeftTimer();
    }

    @Override
    public void initPreferences() {
        duration = durationPreference.getDurationMs();
        durationLeft = duration;
        durationPreference.setValueText(durationToString(duration));
        items.set(0, Pair.create(items.firstElement().first, durationToString(durationLeft)));
        items.setSize(1);
    }

    private String durationToString(long duration) {
        return (DateUtils.secondsToHHMMSSString(duration / 1000));
    }

    @Override
    public boolean onPreferenceClick(Preference arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        return context.getString(R.string.goalDuration);
    }

    private boolean checkFinished() {
        if (offline)
            return true;

        if (durationLeft > 0)
            durationLeft = Math.max(0, duration - session.getDuration());

        return (durationLeft <= 0);
    }

    @Override
    public void updateGui() {
        if (!checkFinished()) {
            items.setSize(1);
            items.set(0, Pair.create(items.firstElement().first, durationToString(durationLeft)));
        }
        overviewItem.update(items);
    }

    private void setFinished() {
        items.setSize(2);
        items.set(
                0,
                Pair.create(context.getString(R.string.goal), context.getString(R.string.goalDuration) + " ("
                        + durationToString(duration) + ")"));
        items.set(
                1,
                Pair.create(context.getString(R.string.goalDurationFinished),
                        Distance.distanceToString(finishedAfter, 2) + " "
                                + GlobalSettings.getInstance().getDistUnit().toShortString()));
    }

    @Override
    protected void putSettings(JSONObject json) throws JSONException {
        duration = json.getLong("duration");
        durationLeft = duration;
        if (json.has("finished")) {
            finishedAfter = json.getDouble("finished");
            setFinished();
        }
    }

    @Override
    protected JSONObject getSettings() {
        JSONObject json = new JSONObject();
        try {
            json.put("duration", duration);
            if (finishedAfter > -1)
                json.put("finished", finishedAfter);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    @Override
    public void addSessionUI(SessionUI sessionUI) {
        if (sessionUI instanceof FragmentSessionOverview)
            ((FragmentSessionOverview) sessionUI).addOverviewItem(overviewItem);
        else if (sessionUI instanceof FragmentSessionOfflineOverview)
            ((FragmentSessionOfflineOverview) sessionUI).addOverviewItem(overviewItem);
    }

    // @Override
    // public void onTimer(boolean resumed) {
    // if (durationLeft <= 0)
    // return;
    //
    // durationLeft = Math.max(0, duration - session.getDuration());
    //
    // overviewItem.setValue(0, durationToString(durationLeft));
    // if (durationLeft == 0) {
    // TTS.getInstance().speak(context.getString(R.string.goalReached), false);
    // finishedAfter = session.getDistance();
    // setFinished();
    // }
    // }

    private void restartTimer(long timeout) {
        timeLeftTimer.cancel();
        timeLeftTimer = new TimeLeftTimer();
        timer.schedule(timeLeftTimer, durationLeft);
    }

    @Override
    public void onStateChanged(Session.State oldState, Session.State newState) {
        if (newState == State.RUNNING) {
            if (!checkFinished()) {
                restartTimer(durationLeft);
            }
        } else if (newState == State.STOPPED) {
            timer.cancel();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference != durationPreference)
            return false;

        initPreferences();
        return true;
    }

    @Override
    protected void init() {
        items = new Vector<Pair<String, String>>();
        if (offline) {
            String value = context.getString(R.string.goalDuration);
            if (duration != 0)
                value = value + " (" + durationToString(duration) + ")";

            items.add(Pair.create(context.getString(R.string.goal), value));
            overviewItem = new OverviewItem(context, items);
            if (finishedAfter > 0)
                setFinished();
        } else {
            Preference preference = new DurationPreference(activity, getAttributeSet(R.layout.goal_duration_preference,
                    "de.badaix.pacetracker.preferences.DurationPreference"));
            durationPreference = (DurationPreference) preference;
            durationPreference.setDefaultValue((long) 30 * 60 * 1000);
            preference.setTitle(activity.getString(R.string.configureDuration));
            items.add(Pair.create(activity.getString(R.string.timeLeft), "N/A"));
            overviewItem = new OverviewItem(context, items);
            preferences.add(preference);
        }
    }

    class TimeLeftTimer extends TimerTask {
        @Override
        public void run() {
            if (checkFinished()) {
                TTS.getInstance().speak(context.getString(R.string.goalReached), false);
                finishedAfter = session.getDistance();
                setFinished();
            } else {
                restartTimer(durationLeft);
            }
        }
    }

}
