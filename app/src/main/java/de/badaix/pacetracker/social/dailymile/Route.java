package de.badaix.pacetracker.social.dailymile;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.social.dailymile.Workout.ActivityType;

public class Route extends JsonSerializable {
    int id = -1;
    ActivityType activityType = ActivityType.UNKNOWN;
    String name = "";
    String location = "";
    String encodedSamples = "";
    Distance distance = null;
    Geo geo = null;

    Route(JSONObject json) throws JSONException {
        if (json.has("id"))
            id = json.getInt("id");
        if (json.has("activity_type"))
            activityType = ActivityType.fromString(json.getString("activity_type"));
        if (json.has("name"))
            name = json.getString("name");
        if (json.has("location"))
            location = json.getString("location");
        if (json.has("encoded_samples"))
            encodedSamples = json.getString("encoded_samples");
        if (json.has("distance"))
            distance = new Distance(json.getJSONObject("distance"));
        if (json.has("geo"))
            geo = new Geo(json.getJSONObject("geo"));
    }

    @Override
    public JSONObject toJson() throws JSONException {
        return null;
    }

    public int getId() {
        return id;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getEncodedSamples() {
        return encodedSamples;
    }

    public Distance getDistance() {
        return distance;
    }

    public Geo getGeo() {
        return geo;
    }

}
