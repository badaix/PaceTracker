package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

public class Location extends JsonSerializable {
    String name = null;

    public Location(JSONObject json) throws JSONException {
        this(json.getString("name"));
    }

    public Location(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("name", name);
        return json;
    }

    @Override
    public String toString() {
        return "name: " + name;
    }
}
