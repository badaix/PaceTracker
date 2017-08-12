package de.badaix.pacetracker.sensor;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.sensor.SensorProvider.ConnectionType;
import de.badaix.pacetracker.settings.JsonSerializable;

public class Sensor implements JsonSerializable {
    private SensorType type;
    private String name;
    public Sensor(SensorType type, String name) {
        this.type = type;
        this.name = name;
    }

    public Sensor(JSONObject json) {
        fromJson(json);
    }

    public SensorType getType() {
        return type;
    }

    public void setType(SensorType type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if ((obj == null) || !(obj instanceof Sensor))
            return false;

        Sensor other = (Sensor) obj;
        return (this.getName().equals(other.getName()) && this.getType().equals(other.getType()));
    }

    @Override
    public void fromJson(JSONObject json) {
        try {
            setName(json.getString("name"));
            setType(SensorType.byType(json.getString("type")));
        } catch (JSONException e) {
            setName("");
            setType(SensorType.NONE);
        }
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("name", getName());
            json.put("type", getType().getType());
        } catch (Exception e) {
        }
        return json;
    }

    public enum SensorType {
        NONE(ConnectionType.NONE, ""), POLAR(ConnectionType.BLUETOOTH, "Polar");// ,
        // ZEPHYR

        private ConnectionType connection;
        private String type;

        SensorType(ConnectionType connection, String type) {
            this.connection = connection;
            this.type = type;
        }

        public static SensorType byType(String type) {
            if (type.equals("Polar"))
                return SensorType.POLAR;

            return SensorType.NONE;
        }

        public ConnectionType getConnection() {
            return connection;
        }

        public String getType() {
            return type;
        }
    }
}
