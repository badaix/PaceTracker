package de.badaix.pacetracker.goal;

import android.app.Activity;
import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.util.Hint;

public class GoalFactory {
    private static Goal goal;

    public static Goal getGoal(Class<? extends Goal> goalType, Activity activity) {
        return getGoal(goalType.getSimpleName(), activity);
    }

    public static Goal getGoal(String goalType, Activity activity) {
        goal = null;
        if (goalType.equals(GoalRoute.class.getSimpleName()))
            goal = new GoalRoute(activity);
        else if (goalType.equals(GoalDistance.class.getSimpleName()))
            goal = new GoalDistance(activity);
        else if (goalType.equals(GoalDuration.class.getSimpleName()))
            goal = new GoalDuration(activity);
        else if (goalType.equals(GoalStandard.class.getSimpleName()))
            goal = new GoalStandard(activity);

        if (goal != null)
            goal.init(activity, false);

        return goal;
    }

    public static Goal getOfflineGoal(Class<? extends Goal> goalType, Context context) {
        return getOfflineGoal(goalType.getSimpleName(), context);
    }

    public static Goal getOfflineGoal(JSONObject json, Context context) {
        Goal goal = null;
        try {
            goal = getOfflineGoal(json.getString("type"), context);
        } catch (JSONException e) {
            Hint.log(GoalFactory.class, e);
        }
        try {
            if ((goal != null) && json.has("settings"))
                goal.initFromJson(json.getJSONObject("settings"));
        } catch (JSONException e) {
            Hint.log(GoalFactory.class, e);
        }
        return goal;
    }

    public static JSONObject toJson(Goal goal) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", goal.getType());
            json.put("settings", goal.getSettings());
        } catch (JSONException e) {
            return null;
        }
        return json;
    }

    public static Goal getOfflineGoal(String goalType, Context context) {
        goal = null;
        if (goalType.equals(GoalRoute.class.getSimpleName()))
            goal = new GoalRoute(null);
        else if (goalType.equals(GoalDistance.class.getSimpleName()))
            goal = new GoalDistance(null);
        else if (goalType.equals(GoalDuration.class.getSimpleName()))
            goal = new GoalDuration(null);
        else if (goalType.equals(GoalStandard.class.getSimpleName()))
            goal = new GoalStandard(null);
        else
            goal = new GoalStandard(null);

        if (goal != null)
            goal.init(context, true);

        return goal;
    }
}
