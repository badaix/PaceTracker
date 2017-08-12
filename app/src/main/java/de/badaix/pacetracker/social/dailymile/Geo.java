package de.badaix.pacetracker.social.dailymile;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.session.GeoPos;

public class Geo extends JsonSerializable {
    GeoType type;
    double latitude;
    double longitude;
    public Geo(JSONObject json) throws JSONException {
        this(GeoType.fromString(json.getString("type")), json.getJSONArray("coordinates").getDouble(0), json
                .getJSONArray("coordinates").getDouble(1));
    }

    public Geo(GeoType type, double latitude, double longitude) {
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public GeoType getType() {
        return type;
    }

    public GeoPos getGeoPos() {
        return new GeoPos(latitude, longitude);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("type", type.toString());
        JSONArray jsonArray = new JSONArray();
        jsonArray.put(latitude);
        jsonArray.put(longitude);
        json.put("coordinates", jsonArray);
        return json;
    }

    public enum GeoType {
        POINT("Point");

        private String asString = "";

        GeoType(String asString) {
            this.asString = asString;
        }

        static GeoType fromString(String type) {
            if (type.equalsIgnoreCase(POINT.toString()))
                return POINT;
            throw new IllegalArgumentException(type);
        }

        @Override
        public String toString() {
            return asString;
        }
    }
}
