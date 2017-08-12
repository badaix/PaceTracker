package de.badaix.pacetracker.social.dailymile;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import de.badaix.pacetracker.R;
import de.badaix.pacetracker.session.Felt;
import de.badaix.pacetracker.util.DateUtils;

public class Workout extends JsonSerializable {

    ActivityType activityType = null;
    Date completedAt = null;
    Distance distance = null;
    Integer duration = null;
    Felt felt = null;
    Integer calories = null;
    String title = null;
    Integer routeId = null;
    Workout(JSONObject json) throws JSONException {
        if (json.has("activity_type"))
            this.activityType = ActivityType.fromString(json.getString("activity_type"));
        if (json.has("completed_at"))
            try {
                this.completedAt = DateUtils.fromISODateString("completed_at");
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        if (json.has("distance"))
            this.distance = new Distance(json.getJSONObject("distance"));
        if (json.has("duration"))
            this.duration = Integer.valueOf(json.getInt("duration"));
        if (json.has("felt"))
            this.felt = Felt.fromString(json.getString("felt"));
        if (json.has("calories"))
            this.calories = Integer.valueOf(json.getInt("calories"));
        if (json.has("title"))
            this.title = json.getString("title");
        if (json.has("route_id"))
            this.routeId = Integer.valueOf(json.getInt("route_id"));
    }

    Workout(ActivityType activityType, Date completedAt, Distance distance, Integer duration, Felt felt,
            Integer calories, String title, Integer routeId) {
        this.activityType = activityType;
        this.completedAt = completedAt;
        this.distance = distance;
        this.duration = duration;
        this.felt = felt;
        this.calories = calories;
        this.title = title;
        this.routeId = routeId;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public Date getCompletedAt() {
        return completedAt;
    }

    public Distance getDistance() {
        return distance;
    }

    public Integer getDuration() {
        return duration;
    }

    public Felt getFelt() {
        return felt;
    }

    public Integer getCalories() {
        return calories;
    }

    public String getTitle() {
        return title;
    }

    public Integer getRouteId() {
        return routeId;
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        json.put("activity_type", activityType.toString());
        if (completedAt != null)
            json.put("completed_at", DateUtils.toISOString(completedAt));
        if (distance != null)
            json.put("distance", distance.toJson());
        if (duration != null)
            json.put("duration", duration);
        if ((felt != null) && (felt != Felt.NONE))
            json.put("felt", felt.toString());
        if (calories != null)
            json.put("calories", calories);
        if (title != null)
            json.put("title", title);
        if (routeId != null)
            json.put("route_id", routeId);
        return json;
    }

    public enum ActivityType {
        UNKNOWN(""), RUNNING("Running"), CYCLING("Cycling"), SWIMMING("Swimming"), WALKING("Walking"), FITNESS(
                "Fitness"), WEIGHTS("Weights"), COMMUTE("Commute"), ELLIPTICAL("Elliptical"), YOGA("Yoga"), CORE_FITNESS(
                "Core Fitness"), CROSS_TRAINING("Cross Training"), HIKING("Hiking"), SPINNING("Spinning"), CROSS_FIT(
                "CrossFit"), ROWING("Rowing"), CC_SKIING("Cc Skiing"), ROCK_CLIMBING("Rock Climbing"), INLINE_SKATING(
                "Inline Skating"), PUSH_UP("Push-up"), FRONT_PLANK("Front Plank"), SIDE_PLANK("Side Plank");

        private String asString = "";

        ActivityType(String asString) {
            this.asString = asString;
        }

        static ActivityType fromString(String type) {
            if (type.equalsIgnoreCase(RUNNING.toString()))
                return RUNNING;
            else if (type.equalsIgnoreCase(CYCLING.toString()))
                return CYCLING;
            else if (type.equalsIgnoreCase(SWIMMING.toString()))
                return SWIMMING;
            else if (type.equalsIgnoreCase(WALKING.toString()))
                return WALKING;
            else if (type.equalsIgnoreCase(FITNESS.toString()))
                return FITNESS;
            else if (type.equalsIgnoreCase(WEIGHTS.toString()))
                return WEIGHTS;
            else if (type.equalsIgnoreCase(COMMUTE.toString()))
                return COMMUTE;
            else if (type.equalsIgnoreCase(ELLIPTICAL.toString()))
                return ELLIPTICAL;
            else if (type.equalsIgnoreCase(YOGA.toString()))
                return YOGA;
            else if (type.equalsIgnoreCase(CORE_FITNESS.toString()))
                return CORE_FITNESS;
            else if (type.equalsIgnoreCase(CROSS_TRAINING.toString()))
                return CROSS_TRAINING;
            else if (type.equalsIgnoreCase(HIKING.toString()))
                return HIKING;
            else if (type.equalsIgnoreCase(SPINNING.toString()))
                return SPINNING;
            else if (type.equalsIgnoreCase(CROSS_FIT.toString()))
                return CROSS_FIT;
            else if (type.equalsIgnoreCase(ROWING.toString()))
                return ROWING;
            else if (type.equalsIgnoreCase(CC_SKIING.toString()))
                return CC_SKIING;
            else if (type.equalsIgnoreCase(ROCK_CLIMBING.toString()))
                return ROCK_CLIMBING;
            else if (type.equalsIgnoreCase(INLINE_SKATING.toString()))
                return INLINE_SKATING;
            else if (type.equalsIgnoreCase(PUSH_UP.toString()))
                return PUSH_UP;
            else if (type.equalsIgnoreCase(FRONT_PLANK.toString()))
                return FRONT_PLANK;
            else if (type.equalsIgnoreCase(SIDE_PLANK.toString()))
                return SIDE_PLANK;
            else {
                ActivityType result = UNKNOWN;
                result.setName(type);
                return result;
            }

            // throw new IllegalArgumentException("Unknown activity type: " +
            // type);
        }

        @Override
        public String toString() {
            return asString;
        }

        public void setName(String name) {
            asString = name;
        }

        public String toLocaleString(Context context) {
            switch (this) {
                case CC_SKIING:
                    return context.getResources().getString(R.string.activityCC_SKIING);
                case COMMUTE:
                    return context.getResources().getString(R.string.activityCOMMUTE);
                case CORE_FITNESS:
                    return context.getResources().getString(R.string.activityCORE_FITNESS);
                case CROSS_FIT:
                    return context.getResources().getString(R.string.activityCROSS_FIT);
                case CROSS_TRAINING:
                    return context.getResources().getString(R.string.activityCROSS_TRAINING);
                case CYCLING:
                    return context.getResources().getString(R.string.activityCYCLING);
                case ELLIPTICAL:
                    return context.getResources().getString(R.string.activityELLIPTICAL);
                case FITNESS:
                    return context.getResources().getString(R.string.activityFITNESS);
                case HIKING:
                    return context.getResources().getString(R.string.activityHIKING);
                case INLINE_SKATING:
                    return context.getResources().getString(R.string.activityINLINE_SKATING);
                case PUSH_UP:
                    return context.getResources().getString(R.string.activityPUSH_UP);
                case ROCK_CLIMBING:
                    return context.getResources().getString(R.string.activityROCK_CLIMBING);
                case ROWING:
                    return context.getResources().getString(R.string.activityROWING);
                case RUNNING:
                    return context.getResources().getString(R.string.activityRUNNING);
                case SPINNING:
                    return context.getResources().getString(R.string.activitySPINNING);
                case SWIMMING:
                    return context.getResources().getString(R.string.activitySWIMMING);
                case WALKING:
                    return context.getResources().getString(R.string.activityWALKING);
                case WEIGHTS:
                    return context.getResources().getString(R.string.activityWEIGHTS);
                case YOGA:
                    return context.getResources().getString(R.string.activityYOGA);

                case FRONT_PLANK:
                    return context.getResources().getString(R.string.activityFRONT_PLANK);

                case SIDE_PLANK:
                    return context.getResources().getString(R.string.activitySIDE_PLANK);

                default:
                    return toString();
            }
        }
    }
}
