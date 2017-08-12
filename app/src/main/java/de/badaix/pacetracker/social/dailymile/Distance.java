package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

public class Distance extends JsonSerializable {
    DistanceUnit unit;
    float value;
    public Distance(JSONObject json) throws JSONException {
        this((float) json.getDouble("value"), DistanceUnit.fromString(json.getString("units")));
    }

    public Distance(float value, DistanceUnit unit) {
        this.value = value;
        this.unit = unit;
    }

    public DistanceUnit getUnit() {
        return unit;
    }

    public float getValue() {
        return value;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("value", de.badaix.pacetracker.util.Distance.doubleToStringNeutral(value, 2));
        json.put("units", unit.toString());
        return json;
    }

    public enum DistanceUnit {
        MILES("miles", "miles"), KILOMETERS("kilometers", "km"), YARDS("yards", "yards"), METERS("meters", "m");

        private String asString = "";
        private String asShortString = "";

        DistanceUnit(String asString, String asShortString) {
            this.asString = asString;
            this.asShortString = asShortString;
        }

        static DistanceUnit fromString(String type) {
            if (type.equalsIgnoreCase(MILES.toString()))
                return MILES;
            else if (type.equalsIgnoreCase(KILOMETERS.toString()))
                return KILOMETERS;
            else if (type.equalsIgnoreCase(YARDS.toString()))
                return YARDS;
            else if (type.equalsIgnoreCase(METERS.toString()))
                return METERS;
            throw new IllegalArgumentException(type);
        }

        @Override
        public String toString() {
            return asString;
        }

        public String toShortString() {
            return asShortString;
        }
    }
}
