package de.badaix.pacetracker.settings;

import org.json.JSONObject;

public interface JsonSerializable {
    public void fromJson(JSONObject json);

    public JSONObject toJson();
}
