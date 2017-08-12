package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.preference.Preference;
import android.util.Pair;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Vector;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.activity.FragmentSessionOfflineOverview;
import de.badaix.pacetracker.activity.FragmentSessionOverview;
import de.badaix.pacetracker.activity.SessionUI;
import de.badaix.pacetracker.preferences.DecimalPreference;
import de.badaix.pacetracker.session.GpsPos;
import de.badaix.pacetracker.settings.GlobalSettings;
import de.badaix.pacetracker.util.DateUtils;
import de.badaix.pacetracker.util.Distance;
import de.badaix.pacetracker.util.TTS;
import de.badaix.pacetracker.views.OverviewItem;

public class GoalDistance extends Goal {
    private double distance = 0;
    private double distanceLeft = 0;
    private OverviewItem overviewItem;
    private DecimalPreference decimalPreference;
    private long finishedAfter = -1;
    private Vector<Pair<String, String>> items;

    GoalDistance(Activity activity) {
        super(activity);
    }

    @Override
    public void initPreferences() {
        distance = decimalPreference.getValue() * GlobalSettings.getInstance().getDistUnit().getFactor();
        distanceLeft = distance;
        decimalPreference.setValueText(Distance.distanceToString(distance, 1) + " "
                + GlobalSettings.getInstance().getDistUnit().toShortString());
        items.set(0, Pair.create(items.firstElement().first, Distance.distanceToString(distance, 2)));
        items.setSize(1);
    }

    @Override
    public boolean onPreferenceClick(Preference arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public String getName() {
        return context.getString(R.string.goalDistance);
    }

    @Override
    public void updateGui() {
        overviewItem.update(items);
    }

    private void setFinished() {
        items.setSize(2);
        items.set(
                0,
                Pair.create(context.getString(R.string.goal), context.getString(R.string.goalDistance) + " ("
                        + Distance.distanceToString(distance, 2) + " "
                        + GlobalSettings.getInstance().getDistUnit().toShortString() + ")"));
        items.set(
                1,
                Pair.create(context.getString(R.string.goalDistanceFinished),
                        DateUtils.secondsToHHMMSSString(finishedAfter / 1000)));
    }

    @Override
    protected void putSettings(JSONObject json) throws JSONException {
        distance = json.getDouble("distance");
        distanceLeft = distance;
        if (json.has("finished")) {
            finishedAfter = json.getLong("finished");
            setFinished();
        }
    }

    @Override
    protected JSONObject getSettings() {
        JSONObject json = new JSONObject();
        try {
            json.put("distance", distance);
            if (finishedAfter != -1)
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

    @Override
    public void onFilteredLocationChanged(GpsPos location) {
        if (distanceLeft <= 0)
            return;

        distanceLeft = Math.max(0, distance - session.getDistance());
        items.set(0, Pair.create(items.firstElement().first, Distance.distanceToString(distanceLeft, 2)));
        if (distanceLeft == 0) {
            TTS.getInstance().speak(context.getString(R.string.goalReached), false);
            finishedAfter = session.getDuration();
            setFinished();
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (preference != decimalPreference)
            return false;

        initPreferences();
        return true;
    }

    @Override
    protected void init() {
        items = new Vector<Pair<String, String>>();
        if (offline) {
            String value = context.getString(R.string.goalDistance);
            if (distance != 0)
                value = value + " (" + Distance.distanceToString(distance, 1) + " "
                        + GlobalSettings.getInstance().getDistUnit().toShortString() + ")";
            items.add(Pair.create(context.getString(R.string.goal), value));
            overviewItem = new OverviewItem(context, items);
            if (finishedAfter > 0)
                setFinished();
        } else {
            Preference preference = new DecimalPreference(activity, getAttributeSet(R.layout.goal_distance_preference,
                    "de.badaix.pacetracker.preferences.DecimalPreference"));
            decimalPreference = (DecimalPreference) preference;
            decimalPreference.setUnit(GlobalSettings.getInstance().getDistUnit().toShortString());
            decimalPreference.setDefaultValue((float) 10);
            preference.setTitle(activity.getString(R.string.configureDistance));
            items.add(Pair.create(activity.getString(R.string.distanceLeft), "N/A"));
            overviewItem = new OverviewItem(context, items);
            preferences.add(preference);
        }
    }

}
