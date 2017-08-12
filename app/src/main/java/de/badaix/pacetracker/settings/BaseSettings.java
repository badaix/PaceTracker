package de.badaix.pacetracker.settings;

import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class BaseSettings implements JsonSerializable {

    public BaseSettings() {
    }

    public BaseSettings(JSONObject jsonSettings) {
        if (jsonSettings != null) {
            fromJson(jsonSettings);
            return;
        }

        loadSettings();
    }

    public JSONObject storeSettings() {
        JSONObject settings = toJson();
        GlobalSettings.getInstance().put(this.getClass().getSimpleName(), settings);
        return settings;
    }

    public BaseSettings loadSettings() {
        String settings = GlobalSettings.getInstance().getString(this.getClass().getSimpleName(), "");

        try {
            if (TextUtils.isEmpty(settings))
                fromJson(null);
            else
                fromJson(new JSONObject(settings));
        } catch (JSONException e) {
            fromJson(null);
        }

        return this;
    }

}
