package de.badaix.pacetracker.session;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;

import de.badaix.pacetracker.settings.JsonSerializable;

public class SessionType implements JsonSerializable {
    String type;
    String name;
    String verb;
    int drawable;
    int lightDrawable;

    public SessionType(JSONObject json) throws JSONException {
        initFromJson(json);
    }

    public SessionType(SessionType sessionType) {
        if (sessionType == null)
            return;
        this.type = sessionType.type;
        this.name = sessionType.name;
        this.verb = sessionType.verb;
        this.drawable = sessionType.drawable;
        this.lightDrawable = sessionType.lightDrawable;
    }

    SessionType(String type, String name, String verb, int drawable, int lightDrawable) {
        this.type = type;
        this.name = name;
        this.verb = verb;
        this.drawable = drawable;
        this.lightDrawable = lightDrawable;
    }

    public String getType() {
        return type;
    }

    public String getName(Context context) {
        return name;
    }

    public String getVerb(Context context) {
        return verb;
    }

    public int getDrawable() {
        return drawable;
    }

    public int getLightDrawable() {
        return lightDrawable;
    }

    private void initFromJson(JSONObject json) {
        try {
            type = json.getString("type");
            name = SessionFactory.getInstance().getSessionNameFromType(type);// json.getString("name");
            verb = SessionFactory.getInstance().getSessionVerbFromType(type);// json.getString("name");
            drawable = SessionFactory.getInstance().getSessionDrawableFromType(type);
            lightDrawable = SessionFactory.getInstance().getSessionLightDrawableFromType(type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void fromJson(JSONObject json) {
        initFromJson(json);
    }

    @Override
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("name", name);
            json.put("verb", verb);
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
        return json;
    }
}
