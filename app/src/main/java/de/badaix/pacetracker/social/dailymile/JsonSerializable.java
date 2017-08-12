package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

public abstract class JsonSerializable {
    public abstract JSONObject toJson() throws JSONException;
}
