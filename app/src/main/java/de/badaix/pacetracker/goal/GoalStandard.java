package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.preference.Preference;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.R;

public class GoalStandard extends Goal {

    public GoalStandard(Activity activity) {
        super(activity);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        return false;
    }

    @Override
    public String getName() {
        return context.getString(R.string.goalStandard);
    }

    @Override
    protected void init() {
    }

    @Override
    protected JSONObject getSettings() {
        return null;
    }

    @Override
    protected void putSettings(JSONObject json) throws JSONException {
    }

}
